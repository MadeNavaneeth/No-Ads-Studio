// Compat shim for the ExperientialLabs gateway (strict OpenAI-compatible validation).
// 1. opencode auto-adds `verbosity: "low"` / `reasoning_effort: "medium"` to any model
//    whose id contains "gpt-5" (matches gpt-5.6-luna). This gateway rejects `verbosity`,
//    so strip the injected provider options before the request goes out.
// 2. Truncate very long tool descriptions as a guardrail: the gateway rejects
//    function descriptions above ~8KB (8192 passes, 10240 fails; probed 2026-09-05).
// NOTE: the `task` tool can't be fixed here — opencode appends the ~12KB subagent
// prompt to its description AFTER this hook — so `task` is denied via
// `"permission": { "task": "deny" }` in opencode.json instead (deny also excludes
// it from the request payload). Subagent delegation is unavailable with this
// provider until the gateway raises its description limit.
export const GatewayCompat = async () => {
  return {
    "chat.params": async (input, output) => {
      if (input?.model?.providerID !== "experientiallabs") return
      if (output?.options) {
        delete output.options.textVerbosity
      }
    },
    "tool.definition": async (input, output) => {
      if (typeof output?.description === "string" && output.description.length > 8000) {
        output.description = output.description.slice(0, 8000)
      }
    },
  }
}
