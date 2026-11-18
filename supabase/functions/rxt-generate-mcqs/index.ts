import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function toBase64(bytes: Uint8Array) {
  let binary = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

function cleanQuestion(value: unknown) {
  return typeof value === "string" ? value.trim().replace(/\s+/g, " ") : "";
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  try {
    const authorization = req.headers.get("Authorization");
    if (!authorization?.startsWith("Bearer ")) return json({ error: "Sign in required" }, 401);

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const publishableKeys = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
    const legacyAnon = Deno.env.get("SUPABASE_ANON_KEY");
    const publishableKey = publishableKeys
      ? JSON.parse(publishableKeys).default
      : legacyAnon;
    const geminiKey = Deno.env.get("GEMINI_API_KEY");
    if (!supabaseUrl || !publishableKey) return json({ error: "Supabase function configuration missing" }, 500);
    if (!geminiKey) return json({ error: "Gemini is not configured yet. Add GEMINI_API_KEY in Supabase Edge Function Secrets." }, 503);

    const supabase = createClient(supabaseUrl, publishableKey, {
      global: { headers: { Authorization: authorization } },
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const { data: userData, error: userError } = await supabase.auth.getUser();
    if (userError || !userData.user) return json({ error: "Invalid or expired session" }, 401);

    const { data: profile, error: profileError } = await supabase
      .from("rxt_profiles")
      .select("role")
      .eq("id", userData.user.id)
      .maybeSingle();

    if (profileError) return json({ error: "Could not verify admin permissions" }, 500);
    if (profile?.role !== "admin") return json({ error: "Only the verified admin can generate questions" }, 403);

    const body = await req.json();
    const noteId = typeof body.note_id === "string" ? body.note_id : "";
    const requestedCount = Number(body.count ?? 10);
    const count = Math.max(1, Math.min(30, Number.isFinite(requestedCount) ? Math.floor(requestedCount) : 10));
    if (!noteId) return json({ error: "note_id is required" }, 400);

    const { data: note, error: noteError } = await supabase
      .from("rxt_notes")
      .select("id, chapter_id, title, file_path")
      .eq("id", noteId)
      .single();
    if (noteError || !note) return json({ error: "PDF note not found or not accessible" }, 404);

    const { data: file, error: fileError } = await supabase.storage
      .from("rxt-notes")
      .download(note.file_path);
    if (fileError || !file) return json({ error: "Could not download the PDF from private storage" }, 400);
    if (file.size > 15 * 1024 * 1024) return json({ error: "PDF is larger than the 15 MB app limit" }, 413);
    if (file.type && file.type !== "application/pdf") return json({ error: "Uploaded file must be a PDF" }, 400);

    const pdfBase64 = toBase64(new Uint8Array(await file.arrayBuffer()));
    const prompt = [
      "You are an accurate Bihar Board school exam question writer.",
      "Read the attached PDF carefully. Create multiple-choice questions using only facts supported by this PDF.",
      "Do not invent facts. If the source is unclear, skip that question.",
      `Create up to ${count} distinct questions. Use the same language as the PDF; if it is mixed, prefer clear Hindi.`,
      "Each question must have exactly four distinct answer options and exactly one correct answer.",
      "correct_option is a zero-based integer from 0 to 3. Provide a concise explanation grounded in the PDF.",
      "Avoid duplicate questions, trick wording, and ambiguous options. Return only JSON matching the schema.",
    ].join("\n");

    const geminiResponse = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": geminiKey },
        body: JSON.stringify({
          contents: [{
            role: "user",
            parts: [
              { text: prompt },
              { inline_data: { mime_type: "application/pdf", data: pdfBase64 } },
            ],
          }],
          generationConfig: {
            temperature: 0.2,
            responseMimeType: "application/json",
            responseSchema: {
              type: "OBJECT",
              properties: {
                questions: {
                  type: "ARRAY",
                  items: {
                    type: "OBJECT",
                    properties: {
                      question: { type: "STRING" },
                      options: { type: "ARRAY", items: { type: "STRING" } },
                      correct_option: { type: "INTEGER" },
                      explanation: { type: "STRING" },
                      difficulty: { type: "STRING", enum: ["easy", "medium", "hard"] },
                    },
                    required: ["question", "options", "correct_option", "explanation", "difficulty"],
                  },
                },
              },
              required: ["questions"],
            },
          },
        }),
      },
    );

    const geminiPayload = await geminiResponse.json();
    if (!geminiResponse.ok) {
      const message = geminiPayload?.error?.message ?? "Gemini request failed";
      return json({ error: message }, geminiResponse.status === 429 ? 429 : 502);
    }

    const rawText = geminiPayload?.candidates?.[0]?.content?.parts
      ?.map((part: { text?: string }) => part.text ?? "")
      .join("");
    if (!rawText) return json({ error: "Gemini returned an empty response" }, 502);

    let parsed: { questions?: unknown[] };
    try {
      parsed = JSON.parse(rawText);
    } catch {
      return json({ error: "Gemini response was not valid JSON. Please retry." }, 502);
    }

    const seen = new Set<string>();
    const validQuestions = (Array.isArray(parsed.questions) ? parsed.questions : [])
      .map((item: any) => ({
        question: cleanQuestion(item?.question),
        options: Array.isArray(item?.options) ? item.options.map(cleanQuestion) : [],
        correct_option: Number(item?.correct_option),
        explanation: cleanQuestion(item?.explanation),
        difficulty: ["easy", "medium", "hard"].includes(item?.difficulty) ? item.difficulty : "medium",
      }))
      .filter((item) => {
        const key = item.question.toLocaleLowerCase();
        if (!item.question || item.options.length !== 4 || item.options.some((v) => !v) ||
            new Set(item.options.map((v) => v.toLocaleLowerCase())).size !== 4 ||
            !Number.isInteger(item.correct_option) || item.correct_option < 0 || item.correct_option > 3 ||
            seen.has(key)) return false;
        seen.add(key);
        return true;
      });

    if (!validQuestions.length) return json({ error: "No valid MCQs could be created from this PDF. Check the PDF and retry." }, 422);

    const rows = validQuestions.map((item) => ({
      chapter_id: note.chapter_id,
      question: item.question,
      options: item.options,
      correct_option: item.correct_option,
      explanation: item.explanation,
      difficulty: item.difficulty,
      source_note_id: note.id,
      is_published: false,
    }));

    const { data: inserted, error: insertError } = await supabase
      .from("rxt_questions")
      .insert(rows)
      .select("id, question, options, correct_option, explanation, difficulty, is_published");

    if (insertError) return json({ error: "Questions were generated but could not be saved. " + insertError.message }, 500);

    return json({
      ok: true,
      note_title: note.title,
      generated: inserted?.length ?? 0,
      questions: inserted ?? [],
      review_required: true,
      message: "Questions saved as drafts. Admin review and publishing are required before students can see them.",
    });
  } catch (error) {
    return json({ error: error instanceof Error ? error.message : "Unexpected server error" }, 500);
  }
});
