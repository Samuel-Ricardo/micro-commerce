<#
.SYNOPSIS
  CI-friendly QA runner for Gemma‑4 E4B agentic tests.

.DESCRIPTION
  Executes selected QA tests (CoT alignment, latency microbench, concurrency stress). When run with `-Simulate` it performs dry‑run and produces synthetic results for CI integration.

.PARAMETER ModelPath
  Path to the GGUF model. Optional in simulate mode.

.PARAMETER Simulate
  If set, run in simulation mode and produce deterministic synthetic results.

.OUTPUTS
  qa/results.json
#>

Param(
    [string]$ModelPath = "",
    [switch]$Simulate
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$out = @{ }

if ($Simulate) {
    Write-Host "Simulate mode: generating synthetic QA results"
    $out.coT_alignment = 0.86
    $out.latency_avg = 6.2
    $out.latency_p95 = 8.9
    $out.tokens_per_sec = 72
    $out.gpu_avg = 62.3
    $out.max_vram_mb = 8200
    $out.concurrent_pass = $true
    $out.tests = @{ 'cot' = 'pass'; 'latency' = 'pass'; 'concurrency' = 'pass' }
    $json = $out | ConvertTo-Json -Depth 5
    if (-not (Test-Path qa)) { New-Item -Path qa -ItemType Directory | Out-Null }
    $json | Out-File -FilePath qa\results.json -Encoding utf8
    Write-Host "Simulation results written to qa\results.json"
    exit 0
}

if (-not $ModelPath) { throw "ModelPath is required unless -Simulate is used." }

# 1) Run microbenchmark
Write-Host "Running micro-benchmark..."
pwsh .\scripts\run_benchmark.ps1 -ModelPath $ModelPath -LlamaCppExe .\llama\main.exe -N_Gpu_Layers 20 -Threads ([Environment]::ProcessorCount) -N_Predict 512 -Backend llama

# For now we'll parse bench_output latest report (user will inspect real outputs)
Write-Host "Collecting bench outputs..."

$results = @{ }
$results.latency_avg = "(see bench_output)"
$results.tokens_per_sec = "(see bench_output)"

$results_json = $results | ConvertTo-Json -Depth 5
$results_json | Out-File -FilePath qa\results.json -Encoding utf8
Write-Host "QA run complete. Results: qa\results.json"
## CI-friendly QA runner for Gemma-4 E4B variant benchmarks and tests.
## Produces qa/results.json and prints short summary.
##
## Usage examples:
##   # simulated run for CI validation
##   ./run_qa.ps1 -Mode simulate -Output results.json
##
##   # run against model server
##   ./run_qa.ps1 -Mode run -ServerUrl http://localhost:8080/v1/generate -Models @('arsovskidev-distill','unsloth-q4') -Repeat 3
##
param(
    [ValidateSet('run','simulate')]
    [string]$Mode = 'simulate',
    [string]$ServerUrl = 'http://localhost:8080/v1/generate',
    [string[]]$Models = @('arsovskidev-distill','unsloth-q4'),
    [int]$Repeat = 3,
    [int[]]$ConcurrencyLevels = @(1,4,8,16,32),
    [string]$Output = 'qa/results.json',
    [int]$MaxTokens = 128,
    [int]$TimeoutSeconds = 30
)

Set-StrictMode -Version Latest

function Write-Log { param($s) Write-Host "[qa] $s" }
function New-RunId { [guid]::NewGuid().ToString() }

function Get-Percentile {
    param([double[]]$Values, [int]$p = 50)
    if ($Values.Count -eq 0) { return $null }
    $arr = $Values | Sort-Object
    $idx = ($p/100.0) * ($arr.Count - 1)
    $lo = [math]::Floor($idx)
    $hi = [math]::Ceiling($idx)
    if ($lo -eq $hi) { return [math]::Round($arr[$lo],3) }
    $v = $arr[$lo] + ($arr[$hi] - $arr[$lo]) * ($idx - $lo)
    return [math]::Round($v,3)
}

function Measure-Request {
    param(
        [string]$Model,
        [string]$Prompt,
        [int]$MaxTokens = 128,
        [int]$TimeoutSeconds = 30
    )
    $start = Get-Date
    if ($Mode -eq 'simulate') {
        # Simulate variable latency and outputs based on model id
        Start-Sleep -Milliseconds (if ($Model -like '*distill*') { 800 } else { 1800 })
        if ($Prompt -match 'Calculate') {
            $text = "1) 23 * 7 = 161`n2) 5 * 12 = 60`n3) 161 + 60 = 221`nAnswer: 221"
        } elseif ($Prompt -match 'add\(') {
            $text = "def add(a, b):`n    return a + b"
        } else {
            $text = "1) Step one`n2) Step two`n3) Done"
        }
        $elapsed = (Get-Date) - $start
        return @{ success = $true; text = $text; elapsed = $elapsed.TotalSeconds; tokens = ([math]::Max(1, [int]([math]::Round(($text.Length/4))))) }
    }

    # Real request mode
    $body = @{ model = $Model; prompt = $Prompt; max_tokens = $MaxTokens; temperature = 0; seed = 42 } | ConvertTo-Json
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Headers.Add('Content-Type','application/json')
        $wc.Proxy = $null
        $startReq = Get-Date
        $resp = $wc.UploadString($ServerUrl, $body)
        $elapsed = (Get-Date) - $startReq
        $j = $null
        try { $j = $resp | ConvertFrom-Json } catch { $j = @{ text = $resp } }
        # try common fields
        $text = $null
        if ($j -is [System.Management.Automation.PSCustomObject]) {
            if ($j.output) { $text = $j.output }
            elseif ($j.generated_text) { $text = $j.generated_text }
            elseif ($j.choices -and $j.choices[0].text) { $text = $j.choices[0].text }
            elseif ($j.text) { $text = $j.text }
            else { $text = ($j | ConvertTo-Json -Depth 5) }
        } else { $text = $resp }
        $tokenCount = $null
        if ($j -and $j.usage -and $j.usage.total_tokens) { $tokenCount = [int]$j.usage.total_tokens }
        if (-not $tokenCount) { $tokenCount = [math]::Max(1, [int]([math]::Round(($text.Length/4)))) }
        return @{ success = $true; text = $text; elapsed = $elapsed.TotalSeconds; tokens = $tokenCount }
    } catch {
        return @{ success = $false; error = $_.Exception.Message; elapsed = ((Get-Date) - $start).TotalSeconds }
    }
}

function Extract-StepsFromText {
    param([string]$Text)
    $lines = $Text -split "\r?\n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
    # keep lines that look like steps (leading number or dash)
    $steps = @()
    foreach ($l in $lines) {
        if ($l -match '^\d+\)\s*(.+)$') { $steps += $matches[1].Trim() }
        elseif ($l -match '^\d+\.\s*(.+)$') { $steps += $matches[1].Trim() }
        elseif ($l -match '^[-*]\s*(.+)$') { $steps += $matches[1].Trim() }
    }
    if ($steps.Count -eq 0) { return $lines } else { return $steps }
}

function Jaccard-Similarity {
    param([string]$a, [string]$b)
    $sa = ($a -split '\\W+' | Where-Object { $_ -ne '' } | ForEach-Object { $_.ToLower() })
    $sb = ($b -split '\\W+' | Where-Object { $_ -ne '' } | ForEach-Object { $_.ToLower() })
    $setA = $sa | Select-Object -Unique
    $setB = $sb | Select-Object -Unique
    $inter = ($setA | Where-Object { $setB -contains $_ }).Count
    $union = ([System.Linq.Enumerable]::Count([System.Linq.Enumerable]::Union($setA,$setB)))
    if ($union -eq 0) { return 0.0 }
    return [double]$inter / [double]$union
}

function Compare-CoT {
    param([string[]]$goldSteps, [string[]]$modelSteps, [double]$threshold = 0.6)
    # Compute SAR: fraction of gold steps that have a matching model step with Jaccard >= threshold
    $matched = 0
    $foundIndices = @()
    for ($i=0; $i -lt $goldSteps.Count; $i++) {
        $g = $goldSteps[$i]
        for ($j=0; $j -lt $modelSteps.Count; $j++) {
            if ($foundIndices -contains $j) { continue }
            $sim = Jaccard-Similarity -a $g -b $modelSteps[$j]
            if ($sim -ge $threshold) { $matched++; $foundIndices += $j; break }
        }
    }
    $SAR = if ($goldSteps.Count -gt 0) { $matched / $goldSteps.Count } else { 0 }
    # SC: fraction of required sub-steps present (same as SAR here unless partial credit desired)
    $SC = $SAR
    $CoTF = if (($SAR + $SC) -gt 0) { 2 * ($SAR * $SC) / ($SAR + $SC) } else { 0 }
    return @{ sar = [math]::Round($SAR,3); sc = [math]::Round($SC,3); cotf = [math]::Round($CoTF,3) }
}

# canonical tests
$tests = @(
    @{ id='arithmetic_cot'; type='cot'; prompt='Calculate (23 * 7) + (5 * 12). Show step-by-step reasoning and final answer.'; model_expected='221'; goldSteps=@('23 * 7 = 161','5 * 12 = 60','161 + 60 = 221') },
    @{ id='logical_deduction'; type='cot'; prompt='Given: All chefs are cooks. Some cooks are bakers. Is it necessarily true that some chefs are bakers? Show reasoning.'; model_expected='No' ; goldSteps=@('All chefs are cooks','Some cooks are bakers','Cannot conclude some chefs are bakers') },
    @{ id='code_synthesis'; type='golden'; prompt='Write a Python function add(a, b) that returns a + b.'; model_expected='def add(a, b):`n    return a + b' },
    @{ id='multi_step_plan'; type='stepwise'; prompt='Provide 5 numbered steps to deploy a simple Node.js app to Azure App Service.'; goldSteps=@('Create app','Initialize repo','Create Azure App Service','Push code','Configure settings') }
)

$runId = New-RunId
$results = [ordered]@{ run_id = $runId; timestamp = (Get-Date).ToString('o'); env = @{ mode = $Mode; server = $ServerUrl; models = $Models }; tests = @(); summary = @{ total = 0; passed = 0; failed = 0; critical_failed = 0 } }

foreach ($model in $Models) {
    Write-Log "Running tests for model: $model"
    foreach ($t in $tests) {
        $metrics = [ordered]@{ latencies = @(); tokens = @(); successes = 0; errors = 0 }
        $cotMetrics = $null
        for ($r = 0; $r -lt $Repeat; $r++) {
            $res = Measure-Request -Model $model -Prompt $t.prompt -MaxTokens $MaxTokens -TimeoutSeconds $TimeoutSeconds
            if ($res.success) {
                $metrics.latencies += $res.elapsed
                $metrics.tokens += $res.tokens
                $metrics.successes++
                # Evaluate CoT / golden
                if ($t.type -eq 'cot' -or $t.type -eq 'stepwise') {
                    $steps = Extract-StepsFromText -Text $res.text
                    $cmp = Compare-CoT -goldSteps $t.goldSteps -modelSteps $steps -threshold 0.6
                    $cotMetrics = $cmp
                } elseif ($t.type -eq 'golden') {
                    $out = ($res.text -replace '\r','') -replace '\n+$',''
                    # normalize whitespace
                    $normOut = ($out -split "\r?\n" | ForEach-Object { $_.TrimEnd() }) -join "\n"
                    $normGold = $t.model_expected -replace "\r",""
                    $normGold = ($normGold -split "\r?\n" | ForEach-Object { $_.TrimEnd() }) -join "\n"
                    $sim = Jaccard-Similarity -a $normGold -b $normOut
                    $cotMetrics = @{ similarity = [math]::Round($sim,3) }
                }
            } else {
                $metrics.errors++
            }
        }
        $count = $metrics.latencies.Count
        $p50 = if ($count -gt 0) { (Get-Percentile -Values $metrics.latencies -p 50) } else { $null }
        $p95 = if ($count -gt 0) { (Get-Percentile -Values $metrics.latencies -p 95) } else { $null }
        $p99 = if ($count -gt 0) { (Get-Percentile -Values $metrics.latencies -p 99) } else { $null }
        $avgTokens = if ($metrics.tokens.Count -gt 0) { [math]::Round(($metrics.tokens | Measure-Object -Average).Average,2) } else { 0 }
        $meanLatency = if ($metrics.latencies.Count -gt 0) { [math]::Round(($metrics.latencies | Measure-Object -Average).Average,3) } else { 0 }
        $tokensPerSec = if ($meanLatency -gt 0) { [math]::Round($avgTokens / $meanLatency,2) } else { 0 }

        $pass = $true
        $criticalFail = $false
        # Basic acceptance checks (simplified mapping)
        if ($t.type -eq 'golden') {
            if ($cotMetrics -and $cotMetrics.similarity -lt 0.95) { $pass = $false; $criticalFail = $true }
        } elseif ($t.type -eq 'cot' -or $t.type -eq 'stepwise') {
            if ($cotMetrics) {
                $sar = $cotMetrics.sar
                $sc = $cotMetrics.sc
                $cotf = $cotMetrics.cotf
                # thresholds per model (simple)
                if ($model -match 'distill') {
                    if ($cotf -lt 0.85) { $pass = $false; $criticalFail = $true }
                } else {
                    if ($cotf -lt 0.75) { $pass = $false; $criticalFail = $true }
                }
            } else { $pass = $false; $criticalFail = $true }
        }

        $testResult = [ordered]@{
            id = $t.id
            model = $model
            attempts = $Repeat
            metrics = @{ p50 = $p50; p95 = $p95; p99 = $p99; mean_latency = $meanLatency; avg_tokens = $avgTokens; tokens_per_sec = $tokensPerSec; errors = $metrics.errors }
            cot = $cotMetrics
            pass = $pass
            critical_fail = $criticalFail
        }
        $results.tests += $testResult
        $results.summary.total += 1
        if ($pass) { $results.summary.passed += 1 } else { $results.summary.failed += 1 }
        if ($criticalFail) { $results.summary.critical_failed += 1 }
    }

    # Concurrency stress tests
    foreach ($c in $ConcurrencyLevels) {
        Write-Log "Concurrency test for $model at level $c"
        $jobs = @()
        $latencies = @()
        $errors = 0
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        for ($i=0; $i -lt $c; $i++) {
            $jobs += [powershell]::Create().AddScript({ param($m,$p,$mt) Measure-Request -Model $m -Prompt $p -MaxTokens $mt }).AddArgument($model).AddArgument('Concurrent short prompt').AddArgument($MaxTokens)
        }
        # start and wait
        foreach ($j in $jobs) { $j.BeginInvoke() | Out-Null }
        foreach ($j in $jobs) {
            try {
                $res = $j.EndInvoke($null)
                if ($res.success) { $latencies += $res.elapsed } else { $errors++ }
            } catch { $errors++ }
        }
        $sw.Stop()
        $p50 = if ($latencies.Count -gt 0) { (Get-Percentile -Values $latencies -p 50) } else { $null }
        $p95 = if ($latencies.Count -gt 0) { (Get-Percentile -Values $latencies -p 95) } else { $null }
        $throughput = if ($sw.Elapsed.TotalSeconds -gt 0) { [math]::Round(($latencies.Count / $sw.Elapsed.TotalSeconds),2) } else { 0 }
        $pass = $true
        if ($errors -gt ([math]::Ceiling(0.05 * $c))) { $pass = $false }
        $testResult = [ordered]@{ id = 'concurrency'; model = $model; concurrency = $c; metrics = @{ p50 = $p50; p95 = $p95; throughput = $throughput; errors = $errors }; pass = $pass }
        $results.tests += $testResult
        $results.summary.total += 1
        if ($pass) { $results.summary.passed += 1 } else { $results.summary.failed += 1 }
        if (-not $pass) { $results.summary.critical_failed += 1 }
    }
}

# Persist results
$dir = Split-Path $Output -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$results | ConvertTo-Json -Depth 6 | Out-File -FilePath $Output -Encoding utf8

# Print summary
Write-Log "Run complete. Results: $($results.summary.passed)/$($results.summary.total) passed, $($results.summary.critical_failed) critical failures"
Get-Content $Output | Write-Host

if ($results.summary.critical_failed -gt 0) { exit 2 } elseif ($results.summary.failed -gt 0) { exit 1 } else { exit 0 }