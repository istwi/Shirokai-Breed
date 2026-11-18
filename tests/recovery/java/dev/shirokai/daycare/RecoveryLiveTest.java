package dev.shirokai.daycare;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonParser;
import dev.shirokai.daycare.config.Settings;
import dev.shirokai.daycare.domain.EggTransfers;
import dev.shirokai.daycare.domain.TrainerState;
import dev.shirokai.daycare.integration.Custody;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

/** Disposable dedicated-server test. Never uses a real player's UUID or storage. */
public class RecoveryLiveTest implements DedicatedServerModInitializer {
    private final ArrayDeque<Runnable> steps = new ArrayDeque<>();
    private final Map<String, String> results = new LinkedHashMap<>();
    private DaycareService service;
    private UUID owner;
    private Pokemon first, second;
    private String firstSnapshot, secondSnapshot;
    private int ticks, wait;
    private boolean running, finished;
    private java.lang.reflect.Field connectionField;
    private Object database;
    private String databaseUrl;
    private final Set<UUID> fixtures = new HashSet<>();

    public void onInitializeServer() {
        if (!Boolean.getBoolean("shirokai.integrationTest")) throw new IllegalStateException("Test requires isolated integrationTest mode");
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            try {
                if (!server.method_3760().method_14571().isEmpty()) throw new IllegalStateException("Tests refuse to run with players connected");
                if (!running) {
                    service = FabricLoader.getInstance().getEntrypoints("server", DedicatedServerModInitializer.class).stream()
                        .filter(x -> x instanceof RecoveryHost).map(x -> ((RecoveryHost)x).recoveryService()).filter(Objects::nonNull).findFirst().orElse(null);
                    if (service == null || !service.ready) { if (++ticks > 2400) throw new IllegalStateException("Service readiness timeout"); return; }
                    running = true; plan();
                }
                RecoveryAccess a = (RecoveryAccess)(Object)service;
                if (owner != null && a.recoveryBusy().contains(owner)) { if (++wait > 400) throw new IllegalStateException("Asynchronous storage timeout"); return; }
                wait = 0;
                if (steps.isEmpty()) { finish(null); server.method_3747(false); return; }
                steps.removeFirst().run();
            } catch (Throwable e) { finish(e); server.method_3747(false); }
        });
    }

    private void check(String name, boolean ok) {
        if (results.put(name, ok ? "passed" : "failed") != null) throw new IllegalStateException("Duplicate test name: " + name);
        if (!ok) throw new AssertionError(name);
    }
    private void reject(String name, Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { check(name, true); return; }
        check(name, false);
    }
    private Pokemon make(String text) { return PokemonProperties.Companion.parse(text).create(null); }
    private PartyStore party() { return (PartyStore)service.pokemon.stores(owner).get(0); }
    private PCStore pc() { return (PCStore)service.pokemon.stores(owner).get(1); }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void putPc(Pokemon p) { Custody.internal(() -> { PokemonStore store = pc(); store.set(store.getFirstAvailablePosition(), p); return null; }); }
    private void fresh(int index) {
        owner = UUID.nameUUIDFromBytes(("zorykai-recovery-isolated-" + index).getBytes(StandardCharsets.UTF_8));
        if (service.trainers.containsKey(owner)) throw new IllegalStateException("Use a fresh runtime directory; fixture already exists");
        fixtures.add(owner);
        ((RecoveryAccess)(Object)service).recoveryFixtures().add(owner);
        TrainerState state = service.state(owner);state.breedSlots = 3;state.incubatorSlots = 2;
    }
    private boolean exact(String snapshot, UUID id) {
        Pokemon p = service.pokemon.find(owner, id);
        if (p == null) return false;
        String actual = service.pokemon.snapshot(p);
        boolean same = JsonParser.parseString(snapshot).equals(JsonParser.parseString(actual));
        if (!same) try {
            Files.writeString(Path.of("snapshot-difference-"+id+".json"),Settings.JSON.toJson(Map.of("expected",JsonParser.parseString(snapshot),"actual",JsonParser.parseString(actual))));
        } catch(Exception e){throw new RuntimeException(e);}
        return same;
    }
    private boolean inPc(UUID id) { for (Pokemon p : pc()) if (p.getUuid().equals(id)) return true; return false; }
    private void cleanState(String name) {
        check(name + "/idle", service.state(owner).operation == null && !((RecoveryAccess)(Object)service).recoveryFailed().contains(owner));
    }
    private TrainerState.Operation operation(String kind, boolean a, boolean b) {
        TrainerState.Operation op = new TrainerState.Operation();op.kind=kind;op.slot=0;
        if (a) {op.pokemon.add(firstSnapshot);op.pokemonIds.add(first.getUuid());}
        if (b) {op.pokemon.add(secondSnapshot);op.pokemonIds.add(second.getUuid());}
        return op;
    }
    private void preparePair(int slot, int iv, boolean item, String name) {
        first = make("magikarp gender=male level=7");second=make("magikarp gender=female level=10");
        first.getIvs().set(Stats.HP, iv); first.getIvs().set(Stats.ATTACK, 31-iv);
        second.getIvs().set(Stats.HP, 31-iv);second.getIvs().set(Stats.DEFENCE, iv);
        // Changing HP IVs in a fixture does not itself clamp current health.
        first.setCurrentHealth(first.getMaxHealth());second.setCurrentHealth(second.getMaxHealth());
        if (item) {
            var key = class_2960.method_60655("cobblemon", "everstone");
            first.swapHeldItem(new class_1799((class_1935)class_7923.field_41178.method_10223(key)), false, false);
        }
        party().set(slot, first);party().set(slot==0?1:0, second);
        firstSnapshot=service.pokemon.snapshot(first);secondSnapshot=service.pokemon.snapshot(second);
        check(name + "/placed", party().get(slot)==first && party().occupied()==2);
        service.integrationStage(owner, operation("DEPOSIT",true,true),false,false);
    }
    private void plan() {
        steps.add(()->{
            owner=UUID.nameUUIDFromBytes("zorykai-migration-schema3".getBytes(StandardCharsets.UTF_8));fixtures.add(owner);
            var state=service.trainers.get(owner);
            check("migration/schema3-loaded-as4",state!=null&&state.schema==4);
            check("migration/purchased-slots-preserved",state.breedSlots==4&&state.incubatorSlots==3);
            ((RecoveryAccess)(Object)service).recoveryPersist(state,"MIGRATION_TEST",null,()->{});
        });
        // Ensure incubator placement stays zero-based and never reuses an occupied slot.
        steps.add(() -> {
            TrainerState state = new TrainerState(UUID.randomUUID(),6,6);
            for (int i=0;i<6;i++) {
                check("incubator/free-slot-"+i, EggTransfers.firstFreeSlot(state)==i);
                TrainerState.Egg e = new TrainerState.Egg();e.id=UUID.randomUUID();e.slot=i;state.eggs.add(e);
            }
            reject("incubator/full",()->EggTransfers.firstFreeSlot(state));
            var source=service.server.method_3739();
            var root=service.server.method_3734().method_9235().getRoot().getChild("daycareadmin");
            check("command/registered",root.getChild("resetslot")!=null);
            check("command/console-admin",root.canUse(source));
            check("command/non-admin-denied",!root.canUse(source.method_9206(0)));
            check("messages/pc-capacity",new Settings.Messages().text.get("full").equals("PC sem espaço. O Pokémon permanece guardado."));
            Settings messages=new Settings();messages.messages=new Settings.Messages();messages.messages.text.put("full","Equipe e PC sem espaço. O Pokémon permanece guardado.");
            PcPolicy.configure(messages);check("messages/old-default-migrated",messages.messages.text.get("full").startsWith("PC sem espaço"));
            messages.messages.text.put("full","Mensagem personalizada");PcPolicy.configure(messages);check("messages/custom-preserved",messages.messages.text.get("full").equals("Mensagem personalizada"));
        });
        int index=0;
        for (int slot=0;slot<6;slot++) for (int iv : new int[]{0,15,31}) for (boolean item : new boolean[]{false,true}) {
            final int n=++index, position=slot, value=iv; final boolean held=item;
            final String name="slot"+(slot+1)+"/iv"+iv+"/item"+item;
            steps.add(()->{fresh(n);preparePair(position,value,held,name);});
            steps.add(()->{
                cleanState(name+"/deposit");
                check(name+"/party-removed",party().occupied()==0);
                check(name+"/custody",Custody.reserved(first.getUuid())&&Custody.reserved(second.getUuid()));
                check(name+"/saved-snapshots",service.state(owner).couple(0).first.equals(firstSnapshot)&&service.state(owner).couple(0).second.equals(secondSnapshot));
                var op=operation("WITHDRAW_PARENT",true,false);op.category="first";service.integrationStage(owner,op,false,false);
            });
            steps.add(()->{
                cleanState(name+"/withdraw-one");
                check(name+"/first-exact-pc",inPc(first.getUuid())&&exact(firstSnapshot,first.getUuid()));
                check(name+"/party-stays-empty",party().occupied()==0);
                check(name+"/one-parent-left",service.state(owner).couple(0).first==null&&service.state(owner).couple(0).secondId.equals(second.getUuid()));
                var op=operation("DEPOSIT_PARENT",true,false);op.category="first";service.integrationStage(owner,op,false,false);
            });
            steps.add(()->{
                cleanState(name+"/replace");check(name+"/parent-removed-from-pc",!inPc(first.getUuid()));
                service.integrationStage(owner,operation("WITHDRAW",true,true),false,false);
            });
            steps.add(()->{
                cleanState(name+"/withdraw-pair");
                check(name+"/both-exact-pc",inPc(first.getUuid())&&inPc(second.getUuid())&&exact(firstSnapshot,first.getUuid())&&exact(secondSnapshot,second.getUuid()));
                check(name+"/slot-free",service.state(owner).couple(0)==null&&party().occupied()==0);
                check(name+"/locks-released",!Custody.reserved(first.getUuid())&&!Custody.reserved(second.getUuid()));
                check(name+"/purchased-slots",service.state(owner).breedSlots==3&&service.state(owner).incubatorSlots==2);
            });
        }
        final int after=index;
        // Reset, pending egg, repeated clicks, durable failure and reload.
        for (int mode=0;mode<5;mode++) {
            final int testMode=mode;final String name="reset/mode"+mode;
            steps.add(()->{fresh(after+1+testMode);preparePair(5,testMode*7,testMode%2==0,name);});
            steps.add(()->{
                cleanState(name+"/deposit");
                if(testMode==4){var op=operation("WITHDRAW_PARENT",true,false);op.category="first";service.integrationStage(owner,op,false,false);}
            });
            steps.add(()->{
                cleanState(name+"/ready");
                if(testMode==1||testMode==2) {
                    Pokemon child=make("magikarp level=1");var egg=new TrainerState.Egg();egg.id=child.getUuid();egg.pokemon=service.pokemon.snapshot(child);egg.remainingMs=123456;egg.incubationMs=234567;egg.slot=-1;
                    service.state(owner).couple(0).pendingEgg=egg;Custody.lock(egg.id,owner);
                }
                if(testMode==2) {
                    for(int i=0;i<2;i++){var e=new TrainerState.Egg();var p=make("magikarp");e.id=p.getUuid();e.pokemon=service.pokemon.snapshot(p);e.slot=i;service.state(owner).eggs.add(e);Custody.lock(e.id,owner);}
                    reject(name+"/full-incubator-refused",()->SlotRecovery.request(service,owner,0));
                    check(name+"/no-journal-on-reject",service.state(owner).operation==null&&service.state(owner).couple(0).pendingEgg!=null);
                    service.state(owner).eggs.clear();
                }
                reject(name+"/unbought-slot",()->SlotRecovery.request(service,owner,5));
                if(testMode==3)((RecoveryAccess)(Object)service).recoveryFailAfterDurable().add(owner);
                SlotRecovery.request(service,owner,0);
                reject(name+"/double-click-refused",()->SlotRecovery.request(service,owner,0));
            });
            steps.add(()->{
                if(testMode==3){
                    check(name+"/failure-keeps-journal",service.state(owner).operation!=null&&((RecoveryAccess)(Object)service).recoveryFailed().contains(owner));
                    check(name+"/delivery-before-failure",inPc(first.getUuid())&&inPc(second.getUuid()));
                    service.integrationReload(owner);
                }
            });
            steps.add(()->{
                cleanState(name+"/confirmed");
                check(name+"/parents-exact-in-pc",inPc(first.getUuid())&&inPc(second.getUuid())&&exact(firstSnapshot,first.getUuid())&&exact(secondSnapshot,second.getUuid()));
                check(name+"/free-and-purchases-kept",service.state(owner).couple(0)==null&&service.state(owner).breedSlots==3&&service.state(owner).incubatorSlots==2);
                check(name+"/no-party-delivery",party().occupied()==0);
                if(testMode==1||testMode==2)check(name+"/egg-preserved",service.state(owner).eggs.size()==1&&service.state(owner).eggs.getFirst().remainingMs==123456&&service.state(owner).eggs.getFirst().incubationMs==234567&&service.state(owner).eggs.getFirst().slot==0);
                reject(name+"/repeat-empty",()->SlotRecovery.request(service,owner,0));
            });
        }
        steps.add(()->{
            fresh(1000);
            Pokemon locked=make("magikarp gender=female");party().set(5,locked);Custody.lock(locked.getUuid(),owner);
            Pokemon replacement=make("ditto");party().set(5,replacement);
            check("guard/party-set-blocked",party().get(5)==locked);
            party().swap(5,0);check("guard/party-swap-blocked",party().get(5)==locked&&party().get(0)==null);
            @SuppressWarnings("rawtypes") PokemonStore store=pc();
            var at=store.getFirstAvailablePosition();store.set(at,locked);
            check("guard/pc-set-blocked",!inPc(locked.getUuid()));
            check("guard/internal-remove",Custody.internal(()->party().remove(locked)));
            Custody.release(locked.getUuid(),owner);
            check("capacity/party-free-does-not-count",StorageSafety.capacity(service.pokemon,owner,1));
            // Fill one small PC box; all other boxes are removed only in the disposable fixture.
            pc().getBoxes().subList(1,pc().getBoxes().size()).clear();
            for(int i=0;i<30;i++)putPc(make("magikarp"));
            check("capacity/pc-full-despite-free-party",!StorageSafety.capacity(service.pokemon,owner,1)&&party().occupied()==0);
            check("capacity/delivery-refuses-full",!StorageSafety.deliver(service.pokemon,owner,service.pokemon.snapshot(make("magikarp"))));
        });
        steps.add(()->{
            fresh(1001);
            var op=new TrainerState.Operation();op.kind="PURCHASE";op.phase="UNCERTAIN";service.state(owner).operation=op;
            reject("reset/pending-payment-not-erased",()->SlotRecovery.request(service,owner,0));
            check("reset/pending-payment-identical",service.state(owner).operation==op);
            service.state(owner).operation=null;
            RecoveryLog.write("TEST_RECOVER_EVENT",owner,null,0,"diagnostic probe");
            ShirokaiDaycare.LOG.error("Intentional diagnostic test",new IllegalStateException("recovery-test-marker"));
        });
        steps.add(()->{
            fresh(1002);
            Pokemon child=make("magikarp level=1");first=child;firstSnapshot=service.pokemon.snapshot(child);
            var egg=new TrainerState.Egg();egg.id=child.getUuid();egg.pokemon=firstSnapshot;egg.slot=0;egg.remainingMs=0;
            service.state(owner).eggs.add(egg);Custody.lock(egg.id,owner);
            var op=new TrainerState.Operation();op.kind="COLLECT";op.egg=egg.id;op.pokemon.add(firstSnapshot);op.pokemonIds.add(egg.id);
            service.integrationStage(owner,op,false,false);
        });
        steps.add(()->{
            cleanState("collect/confirmed");check("collect/ready-child-exact-pc",inPc(first.getUuid())&&exact(firstSnapshot,first.getUuid()));
            check("collect/party-empty-and-incubator-free",party().occupied()==0&&service.state(owner).eggs.isEmpty());
            var op=new TrainerState.Operation();op.kind="COLLECT";op.egg=first.getUuid();op.pokemon.add(firstSnapshot);op.pokemonIds.add(first.getUuid());
            service.integrationStage(owner,op,false,false);
        });
        steps.add(()->{
            cleanState("collect/idempotent-retry");check("collect/no-duplicate",service.pokemon.list(owner).size()==1&&exact(firstSnapshot,first.getUuid()));
        });
        steps.add(()->{fresh(1003);preparePair(5,0,true,"sqlite-failure");});
        steps.add(()->{
            cleanState("sqlite-failure/deposit");
            try {
                var field=DaycareService.class.getDeclaredField("repository");field.setAccessible(true);database=field.get(service);
                connectionField=database.getClass().getDeclaredField("connection");connectionField.setAccessible(true);
                var connection=(java.sql.Connection)connectionField.get(database);databaseUrl=connection.getMetaData().getURL();connection.close();
            }catch(Exception e){throw new RuntimeException(e);}
            SlotRecovery.request(service,owner,0);
        });
        steps.add(()->{
            check("sqlite-failure/blocks-with-journal",service.state(owner).operation!=null&&((RecoveryAccess)(Object)service).recoveryFailed().contains(owner));
            check("sqlite-failure/no-delivery-before-intent-save",!inPc(first.getUuid())&&!inPc(second.getUuid())&&service.state(owner).couple(0)!=null);
            try {
                var restored=java.sql.DriverManager.getConnection(databaseUrl);
                try(var statement=restored.createStatement()){statement.execute("PRAGMA synchronous=FULL");statement.execute("PRAGMA busy_timeout=5000");}
                connectionField.set(database,restored);
            }catch(Exception e){throw new RuntimeException(e);}
            service.integrationReload(owner);
        });
        steps.add(()->{
            cleanState("sqlite-failure/canonical-reload");
            check("sqlite-failure/old-couple-retained",service.state(owner).couple(0)!=null&&service.state(owner).operation==null);
            SlotRecovery.request(service,owner,0);
        });
        steps.add(()->{
            cleanState("sqlite-failure/retry-confirmed");
            check("sqlite-failure/exact-parents-returned",inPc(first.getUuid())&&inPc(second.getUuid())&&exact(firstSnapshot,first.getUuid())&&exact(secondSnapshot,second.getUuid()));
        });
    }

    private void finish(Throwable error) {
        finished=true;
        try {
            if(error!=null){results.put("UNEXPECTED_FAILURE",error.toString());error.printStackTrace();}
            var report=new LinkedHashMap<String,Object>();report.put("passed",error==null);report.put("checks",results);report.put("assertion_count",results.size());
            report.put("scope","Real Minecraft 1.21.1, Fabric 0.17.2 and Cobblemon 1.8.3 stores, Daycare bytecode and SQLite. Disposable no-player fixtures. Impactor/LuckPerms APIs available only; no payment provider, permissions-provider, authenticated login or rendering test.");
            Files.writeString(Path.of("recovery-test-results.json"),Settings.JSON.toJson(report));
            System.out.println(error==null?"ZORYKAI_RECOVERY_TESTS_PASSED "+results.size():"ZORYKAI_RECOVERY_TESTS_FAILED");
            for(UUID id:fixtures){Custody.releaseOwner(id);service.integrationEnd(id);}
        }catch(Exception e){e.printStackTrace();}
    }
}
