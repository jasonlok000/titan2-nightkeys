import com.everyonesniche.nightkeys.core.*;
import java.util.*;

/** Dependency-free tests for the exact policy/session classes used in the APK. */
public final class CoreTests {
    interface Action { void run() throws Exception; }
    static int passed, assertions;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static void equal(Object a,Object b,String why){check(Objects.equals(a,b),why+": "+a+" != "+b);}
    static void expectFailure(Action a)throws Exception{boolean failed=false;try{a.run();}catch(Exception e){failed=true;}check(failed,"Expected failure");}
    static void test(String name,Action run)throws Exception{try{run.run();passed++;System.out.println("PASS  "+name);}catch(Throwable e){System.err.println("FAIL  "+name);throw e;}}

    static LightPolicy policy(){var p=new LightPolicy(10,15,1200,350);p.setEnabled(true);p.setInteractive(true);return p;}
    static LightPolicy litPolicy(){var p=policy();p.sample(2,0);p.tick(1200);return p;}

    static final class MemoryJournal implements BacklightSession.Journal {
        BacklightSession.Snapshot saved; boolean failSave,failClear; final List<String> trace;
        MemoryJournal(List<String> trace){this.trace=trace;}
        public BacklightSession.Snapshot load(){return saved;}
        public void save(BacklightSession.Snapshot s)throws Exception{trace.add("JOURNAL");if(failSave)throw new Exception("disk");saved=s;}
        public void clear()throws Exception{trace.add("CLEAR");if(failClear)throw new Exception("disk");saved=null;}
    }
    static final class FakePort implements BacklightSession.Port {
        final List<String> trace; String timeout="30000",failOperation; boolean interactive=true,flipScreenOnEnable,verify=true;
        FakePort(List<String> trace){this.trace=trace;}
        void event(String e)throws Exception{trace.add(e);if(Objects.equals(failOperation,e))throw new Exception("injected "+e);}
        public String readTimeout()throws Exception{event("READ");return timeout;}
        public void enableContinuous()throws Exception{event("ENABLE");timeout=BacklightSession.CONTINUOUS;if(flipScreenOnEnable)interactive=false;}
        public void disableLighting()throws Exception{event("DISABLE");timeout="0";}
        public boolean verifyContinuous()throws Exception{event("VERIFY");return verify&&BacklightSession.CONTINUOUS.equals(timeout)&&interactive;}
        public void restore(String original)throws Exception{event("RESTORE");timeout=original;}
        public boolean interactive(){return interactive;}
    }
    static final class Fixture {
        final List<String> trace=new ArrayList<>(); final FakePort port=new FakePort(trace); final MemoryJournal journal=new MemoryJournal(trace);
        final BacklightSession session=new BacklightSession(port,journal);
        void baseline(){equal(port.timeout,"30000","original timeout");check(journal.saved==null,"journal cleared");}
    }

    public static void main(String[]args)throws Exception{
        test("dark requires debounce",()->{var p=policy();p.sample(1,0);check(!p.shouldLight(),"lit immediately");p.tick(1199);check(!p.shouldLight(),"lit early");p.tick(1200);check(p.shouldLight(),"did not light");});
        test("bright turns off after shorter debounce",()->{var p=litPolicy();p.sample(20,2000);p.tick(2349);check(p.shouldLight(),"off early");p.tick(2350);check(!p.shouldLight(),"did not turn off");});
        test("hysteresis retains dark state",()->{var p=litPolicy();p.sample(12,2000);p.tick(10000);check(p.shouldLight(),"hysteresis lost");});
        test("hysteresis retains bright state",()->{var p=policy();p.sample(20,0);p.sample(12,1000);p.tick(10000);check(!p.shouldLight(),"hysteresis lit");});
        test("screen off fails dark",()->{var p=litPolicy();p.setInteractive(false);check(!p.shouldLight(),"screen-off lit");});
        test("screen wake requires fresh sensor sample",()->{var p=litPolicy();p.setInteractive(false);p.setInteractive(true);p.tick(50000);check(!p.shouldLight(),"stale sample reused");});
        test("disabled policy is off",()->{var p=litPolicy();p.setEnabled(false);check(!p.shouldLight(),"disabled lit");});
        test("NaN light fails off",()->{var p=litPolicy();p.sample(Float.NaN,2000);check(!p.shouldLight(),"NaN remained on");});
        test("infinity light fails off",()->{var p=litPolicy();p.sample(Float.POSITIVE_INFINITY,2000);check(!p.shouldLight(),"infinity remained on");});
        test("negative light fails off",()->{var p=litPolicy();p.sample(-1,2000);check(!p.shouldLight(),"negative remained on");});
        test("sensor recovers through debounce",()->{var p=litPolicy();p.sample(Float.NaN,2000);p.sample(0,3000);p.tick(4199);check(!p.shouldLight(),"early recovery");p.tick(4200);check(p.shouldLight(),"no recovery");});
        test("screen-off samples ignored",()->{var p=policy();p.setInteractive(false);p.sample(0,0);p.setInteractive(true);p.tick(5000);check(!p.shouldLight(),"accepted off-screen sample");});
        test("stable screen state does not reset policy",()->{var p=litPolicy();p.setInteractive(true);check(p.shouldLight(),"stable screen reset");});
        test("debounce deadline exposed",()->{var p=policy();equal(p.nextDeadline(),-1L,"idle");p.sample(1,100);equal(p.nextDeadline(),1300L,"deadline");});
        test("clock reversal restarts debounce",()->{var p=policy();p.sample(0,1000);p.tick(500);check(!p.shouldLight(),"reversal lit");p.tick(1700);check(p.shouldLight(),"recovery failed");});
        test("invalid policy rejected",()->{expectFailure(()->new LightPolicy(0,15,1,1));expectFailure(()->new LightPolicy(10,10,1,1));expectFailure(()->new LightPolicy(Float.NaN,15,1,1));});
        test("100000 randomized events respect gates",()->{var random=new Random(62026);var p=policy();boolean enabled=true,screen=true;long now=0;for(int i=0;i<100000;i++){now+=random.nextInt(1000);int kind=random.nextInt(6);if(kind==0){enabled=random.nextBoolean();p.setEnabled(enabled);}else if(kind==1){screen=random.nextBoolean();p.setInteractive(screen);}else if(kind==2)p.sample(Float.NaN,now);else if(kind<=4)p.sample(random.nextFloat()*100,now);else p.tick(now);if(!screen||!enabled)check(!p.shouldLight(),"unsafe event "+i);check(!p.shouldLight()||Float.isFinite(p.lux()),"invalid lux while on");}});

        test("journal precedes first vendor mutation",()->{var f=new Fixture();f.session.prepare();check(f.trace.indexOf("JOURNAL")<f.trace.indexOf("DISABLE"),"write before journal");});
        test("prepare fail-closes with timeout zero",()->{var f=new Fixture();f.session.prepare();equal(f.port.timeout,"0","prepare not off");check(f.journal.saved!=null,"missing restore point");});
        test("dark state writes continuous integer max",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);equal(f.port.timeout,BacklightSession.CONTINUOUS,"not continuous");check(f.session.isLit(),"flag");});
        test("stable dark state causes no repeated writes",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.trace.clear();for(int i=0;i<100;i++)f.session.apply(true);check(f.trace.isEmpty(),"redundant writes");});
        test("bright state writes vendor disabled zero",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.session.apply(false);equal(f.port.timeout,"0","bright not disabled");check(!f.session.isLit(),"flag");});
        test("screen off writes disabled zero",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.port.interactive=false;f.session.apply(true);equal(f.port.timeout,"0","screen off timeout");check(!f.session.isLit(),"screen flag");});
        test("screen-off race after enable is closed",()->{var f=new Fixture();f.session.prepare();f.port.flipScreenOnEnable=true;f.session.apply(true);equal(f.port.timeout,"0","race not disabled");check(!f.session.isLit(),"race flag");});
        test("finish restores exact original timeout",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.session.finish();f.baseline();});
        test("original zero restores as zero",()->{var f=new Fixture();f.port.timeout="0";f.session.prepare();f.session.apply(true);f.session.finish();equal(f.port.timeout,"0","zero changed");});
        test("legacy minus-one restore value accepted",()->{var f=new Fixture();f.port.timeout="-1";f.session.prepare();f.session.finish();equal(f.port.timeout,"-1","legacy restore changed");});
        test("journal save failure prevents vendor mutation",()->{var f=new Fixture();f.journal.failSave=true;expectFailure(f.session::prepare);check(!f.trace.contains("DISABLE"),"mutated without backup");});
        test("unreadable timeout prevents vendor mutation",()->{var f=new Fixture();f.port.timeout=null;expectFailure(f.session::prepare);check(!f.trace.contains("DISABLE"),"mutated unknown timeout");});
        test("failed restore retains journal",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.port.failOperation="RESTORE";expectFailure(f.session::finish);check(f.journal.saved!=null,"journal lost");});
        test("failed restore can be retried",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.port.failOperation="RESTORE";expectFailure(f.session::finish);f.port.failOperation=null;f.session.finish();f.baseline();});
        test("crash journal is healed before new snapshot",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);var next=new BacklightSession(f.port,f.journal);next.prepare();equal(f.journal.saved.timeout,"30000","original restore point lost");next.finish();f.baseline();});
        test("journal clear failure remains recoverable",()->{var f=new Fixture();f.session.prepare();f.journal.failClear=true;expectFailure(f.session::finish);check(f.journal.saved!=null,"journal lost");});
        test("external timeout change stops automation",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.port.timeout="5000";expectFailure(f.session::renew);check(!f.session.isLit(),"flag stayed lit");});
        test("screen-off verify loss is not reported as interactive fault",()->{var f=new Fixture();f.session.prepare();f.session.apply(true);f.port.interactive=false;f.port.verify=false;f.session.renew();check(!f.session.isLit(),"off-screen flag");});
        test("invalid timeout syntax rejected",()->{for(String x:new String[]{"null","abc","-2","2147483648",""})expectFailure(()->BacklightSession.requireTimeout(x));BacklightSession.requireTimeout("0");BacklightSession.requireTimeout("30000");BacklightSession.requireTimeout("2147483647");BacklightSession.requireTimeout("-1");});
        test("repeated finish is idempotent",()->{var f=new Fixture();f.session.prepare();f.session.finish();f.trace.clear();f.session.finish();check(f.trace.isEmpty(),"repeated restore wrote");});

        test("diagnostic hold writes once and preserves original journal",()->{var f=new Fixture();var d=new DiagnosticSession(f.port,f.journal);String original=d.startContinuousHold();equal(original,"30000","diagnostic original");equal(f.port.timeout,BacklightSession.CONTINUOUS,"diagnostic hold");equal(f.journal.saved.timeout,"30000","diagnostic journal");f.trace.clear();check(f.trace.isEmpty(),"unexpected diagnostic writes");});
        test("diagnostic hold does not force screen-off mutation",()->{var f=new Fixture();var d=new DiagnosticSession(f.port,f.journal);d.startContinuousHold();f.port.interactive=false;equal(f.port.timeout,BacklightSession.CONTINUOUS,"hold changed on screen-off without caller");});
        test("diagnostic force-off writes zero",()->{var f=new Fixture();var d=new DiagnosticSession(f.port,f.journal);d.startForcedOff();equal(f.port.timeout,"0","diagnostic off");equal(f.journal.saved.timeout,"30000","diagnostic off journal");});
        test("diagnostic restore returns exact original and clears journal",()->{var f=new Fixture();var d=new DiagnosticSession(f.port,f.journal);d.startContinuousHold();String restored=d.restore();equal(restored,"30000","diagnostic restored value");f.baseline();});
        test("diagnostic refuses a second hardware test while journal pending",()->{var f=new Fixture();var d=new DiagnosticSession(f.port,f.journal);d.startContinuousHold();expectFailure(d::startForcedOff);equal(f.port.timeout,BacklightSession.CONTINUOUS,"second diagnostic mutated");});
        test("diagnostic failed restore retains journal",()->{var f=new Fixture();var d=new DiagnosticSession(f.port,f.journal);d.startForcedOff();f.port.failOperation="RESTORE";expectFailure(d::restore);check(f.journal.saved!=null,"diagnostic journal lost");});
        System.out.println("\nRESULT: "+passed+" tests passed; "+assertions+" assertions. No Android hardware is exercised by this JVM suite.");
    }
}
