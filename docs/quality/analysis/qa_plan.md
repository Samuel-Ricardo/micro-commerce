# QA Plan: Agentic Workflows with Gemma‑4 E4B (GGUF) on RX 6750 XT

Objective
---------
Validate that selected Gemma‑4 E4B quantized variants provide reliable chain‑of‑thought (CoT) reasoning, acceptable latency, and stable multi‑agent concurrency on a single RX 6750 XT (12GB).

Acceptance Criteria
-------------------
- Reasoning fidelity: model CoT outputs match golden traces on a curated set of 40 prompts with >= 80% alignment score (see metric below).
- Latency: single‑turn 512‑token generation completes in <= 10s wall time for Q4 variants (target) and <= 25s for Q8 variants.
- Throughput: steady tokens/sec >= 50 tokens/sec for Q4 on the device (measured via micro‑benchmark).
- Stability: concurrent 4‑agent stress test (4 parallel single‑turn requests) runs for 100 iterations with no OOM or crash.

Metrics & Tools
---------------
- CoT Alignment Score: token‑level overlap of ordered reasoning steps vs golden CoT traces; measure via longest common subsequence (LCS) normalized by gold length.
- Latency: wall time (s) measured by harness; average and 95th percentile.
- Throughput: tokens/sec measured by `bench/benchmark.py`.
- GPU Utilization: average % GPU Engine utilization from `Get-Counter` sampling.
- Memory: max VRAM resident measured by vendor tools (Windows Task Manager GPU engine, or Radeon Software). Log peak memory.

Test Cases
----------
1. CoT correctness: 40 curated prompts covering planning, code reasoning, QA scenarios (agentic tasks). Compare outputs to golden traces.
2. Latency micro‑benchmark: generate 512 tokens from a 64‑token prompt (single run, repeated 10x).
3. Multi‑agent stress: 4 parallel processes each generating 256 tokens, repeat 100 iterations.
4. Edge cases: very long context (64k tokens simulated using chunking) to test offload behavior.

CI Integration
--------------
- `qa/run_qa.ps1` runs tests, saves `qa/results.json`, and sets exit code nonzero on failure.
- For CI, run with `-Simulate` to avoid long GPU tests; full runs should be done on the target machine.

Reporting
---------
- `qa/results.json` includes metrics: coT_alignment, latency_stats, tokens_per_sec, gpu_avg, max_vram_mb, pass/fail per test.
# QA Plan — Agentic workflows with compact Gemma‑4 E4B variants

Purpose
- Define acceptance criteria, test cases, measurement methodology, and a CI-friendly benchmark runner for agentic software workflows using compact Gemma‑4 E4B variants (examples: `arsovskidev-distill`, `unsloth-q4`).
- Enable deterministic golden-output checks, Chain-of-Thought (CoT) correctness metrics, latency & throughput SLAs on AMD Radeon RX 6750 XT, and stress tests for multi-agent concurrent prompts.

Assumptions
- Model deployment exposes an HTTP POST generation endpoint (configurable) that accepts JSON with fields such as `model`, `prompt`/`input`, `max_tokens`, `temperature`, `seed` and returns JSON containing generated text (common keys: `text`, `choices[0].text`, `output`, or `generated_text`).
- Token estimation: when the server doesn't return token counts, tokens ≈ characters / 4 (document this in results).

Metrics & Definitions
- Latency (per-request): wall-clock time from request send to full response received.
- Tokens/sec: tokens generated divided by elapsed seconds (measured per request and aggregated).
- Per-token latency: latency / token_count.
- p50/p95/p99: percentiles of latency distribution across repeated runs.
- Stepwise Alignment Rate (SAR): fraction of reference CoT steps that have a sufficiently similar matching step in the model's CoT (default fuzzy-match threshold 0.60).
- Stepwise Completeness (SC): fraction of required sub-steps present in generated CoT.
- Chain-of-Thought Fidelity (CoTF): harmonic mean of SAR and SC: CoTF = 2*(SAR*SC)/(SAR+SC).
- Error rate: fraction of requests that returned HTTP errors, timeouts, or unparsable outputs.

Acceptance Criteria (per-model)
- Model: `arsovskidev-distill` (distilled E4B)
  - Single-stream throughput (128-token generation): target >= 120 tokens/sec (warning 90–120; fail < 90)
  - End-to-end latency for 128 tokens: target <= 1.5 s (fail > 1.8 s)
  - Per-token latency: p50 <= 12 ms; p95 <= 40 ms
  - CoT: SAR >= 0.85; SC >= 0.90; CoTF >= 0.85

- Model: `unsloth-q4` (Q4 GGUF compact)
  - Single-stream throughput (128-token generation): target >= 60 tokens/sec (warning 40–60; fail < 40)
  - End-to-end latency for 128 tokens: target <= 3.0 s (fail > 4.0 s)
  - Per-token latency: p50 <= 20 ms; p95 <= 80 ms
  - CoT: SAR >= 0.75; SC >= 0.85; CoTF >= 0.75

Concurrency / Stress SLAs (RX 6750 XT, single card)
- Concurrency sweep levels: [1, 4, 8, 16, 32]. Capture aggregate throughput and per-request p50/p95/p99 latency per level.
- Retention targets relative to single-stream throughput:
  - concurrency=8: median per-request throughput >= 60% of single-stream throughput
  - concurrency=16: median per-request throughput >= 50%
  - concurrency=32: median per-request throughput >= 30% (expecting degradation)
- Error rate: < 2% across all concurrency levels (fail if >= 5%).

Automated checks
- Prompt unit tests
  - Validate template rendering and token length safety (pre-flight: prompt token estimate < configured max_tokens)
  - For deterministic tests set `temperature=0` and a fixed `seed`.

- Golden outputs
  - For deterministic, short outputs (code snippets, small tables, arithmetic) use normalized exact-match comparisons.
  - For longer prose/guides use normalized similarity (word-overlap/Jaccard) with thresholds per-case.

- CoT checks
  - Extract numbered/bulleted steps from model output and compare them to golden CoT steps using fuzzy word-overlap (Jaccard). Compute SAR, SC and CoTF and compare against thresholds.

Canonical test cases
- arithmetic_cot
  - Type: CoT (deterministic)
  - Prompt: "Calculate (23 * 7) + (5 * 12). Show step-by-step reasoning and final answer."
  - Golden final: 221
  - Golden CoT: ["23 * 7 = 161","5 * 12 = 60","161 + 60 = 221"]

- logical_deduction
  - Type: CoT
  - Prompt: "Given: All chefs are cooks. Some cooks are bakers. Is it necessarily true that some chefs are bakers? Show reasoning."
  - Expected: correct conclusion with supporting steps. SAR >= 0.80.

- code_synthesis
  - Type: Golden (code)
  - Prompt: "Write a Python function add(a, b) that returns a + b." (temperature=0)
  - Golden: canonical code block `def add(a, b):\n    return a + b` (normalized exact-match or similarity >= 0.95).

- multi_step_plan
  - Type: Stepwise
  - Prompt: "Provide 5 numbered steps to deploy a simple Node.js app to Azure App Service." (expect concrete, actionable steps)
  - SC >= 0.90.

- multi_agent_concurrent
  - Type: Stress
  - Prompts: a mix of short CoT, code, and instruction-following prompts sent concurrently to emulate multiple agents interacting with the model.
  - Measure throughput, latency percentiles, and error rate at concurrency levels.

Measurement guidance
- Repeat each sequential test at least 3 times; report mean, median (p50), p95 and p99 latencies.
- Repeat each concurrency level at least 2 times and aggregate.
- Use deterministic `temperature=0` and `seed` where possible for golden/CoT tests.

CI integration
- The runner `qa/run_qa.ps1` performs the tests and emits a structured JSON report to `qa/results.json` by default and prints a short summary to stdout. The script exits `0` when all critical acceptance checks pass, non-zero otherwise.

Result JSON schema (example)
```
{
  "run_id": "uuid",
  "timestamp": "ISO-8601",
  "env": { "server_url": "...", "models": [...], "hardware": "RX 6750 XT (note)" },
  "tests": [ { /* per-test metrics */ } ],
  "summary": { "total": 10, "passed": 10, "failed": 0, "critical_failed": 0 }
}
```

Notes and next steps
- The included runner supports a `-Mode simulate` to validate CI wiring without a model server.
- For higher accuracy in concurrency benchmarks prefer a runner with native task parallelism (PowerShell 7 `ForEach-Object -Parallel`) or a dedicated load generator to avoid job startup overhead.
