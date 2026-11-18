"""Run meaningful server and diagnostics checks with verified command outcomes.
Usage: python3 tools/run_recovery_tests.py tools-directory fresh-runtime-directory [minecraft-toolchain-runtime]
"""
from pathlib import Path
import hashlib
import json
import subprocess
import sys

repo=Path(__file__).resolve().parents[1]
tools=Path(sys.argv[1]).resolve()
runtime=Path(sys.argv[2]).resolve()
base=Path(sys.argv[3] if len(sys.argv)>3 else '/workspace/shirokai-runtime').resolve()
if (runtime/'world').exists():raise ValueError('Use a fresh test directory; existing world will not be overwritten')
candidate=tools/'recovery-candidate.jar'
digest=hashlib.sha256(candidate.read_bytes()).hexdigest()
subprocess.run(['python3',str(repo/'tools/prepare_recovery_tests.py'),str(tools),str(runtime)],check=True)
cp=str(base/'loader-libraries/*')+':'+str(base/'server.jar')
with (runtime/'recovery-run.log').open('w') as log:
    subprocess.run(['java','-Xms512M','-Xmx4G','-Xverify:all','--add-modules','java.sql','-Dshirokai.integrationTest=true',
                    '-cp',cp,'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui'],cwd=runtime,stdout=log,stderr=subprocess.STDOUT,timeout=600,check=True)
report=json.loads((runtime/'recovery-test-results.json').read_text())
if not report['passed'] or report['assertion_count']<650 or any(v!='passed' for v in report['checks'].values()):
    raise AssertionError('Server checks failed; inspect '+str(runtime/'recovery-test-results.json'))
if hashlib.sha256(candidate.read_bytes()).hexdigest()!=digest:raise AssertionError('Candidate changed during testing')

# The validation-only JAR differs in dependency metadata, not the tested bytecode.
import zipfile
with zipfile.ZipFile(candidate) as a,zipfile.ZipFile(runtime/'mods/daycare-validation-only.jar') as b:
    assert set(a.namelist())==set(b.namelist())
    assert all(a.read(n)==b.read(n) for n in a.namelist() if n!='fabric.mod.json')
diagcp=str(runtime/'test-classes')+':'+str(candidate)+':'+(tools/'recovery-classpath.txt').read_text()
diagwork=runtime/'diagnostics-work';diagwork.mkdir(exist_ok=True)
result=subprocess.run(['java','--add-modules','java.sql','-cp',diagcp,'dev.shirokai.daycare.DiagnosticsTest'],cwd=diagwork,text=True,capture_output=True,check=True)
if 'PASS: 403 JSON records' not in result.stdout:raise AssertionError('Diagnostics tests did not complete')
(runtime/'diagnostics-test.log').write_text(result.stdout+result.stderr)
report['candidate_sha256']=digest
report['diagnostics_test']='passed: 403 JSON records, exception capture, rotation, retention, unrelated-file preservation'
(runtime/'recovery-test-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('PASS:',report['assertion_count'],'live-server assertions; diagnostics checks passed; SHA256',digest)
