import "jsr:@supabase/functions-js/edge-runtime.d.ts";
Deno.serve((req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
    }});
  }
  return new Response(JSON.stringify({
    error: "PDF-to-AI generation is disabled pending a provider whose terms permit this student app and its age group."
  }), {
    status: 403,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
});
