package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ScreenUtils
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.domain.assembly.*
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.refining.*
import fr.solremi.minerspace.domain.robot.*
import fr.solremi.minerspace.domain.services.*
import ktx.app.KtxScreen

class RobotFleetScreen(private val services: GameServices, private val onBack: () -> Unit) : KtxScreen {
    private val economyDef=CoreEconomyContentLoader().load(services.content); private val economy=CoreEconomyEngine(economyDef)
    private val definitions=RobotContentLoader().load(services.content); private val engine=RobotAutomationEngine(definitions); private val ui=RobotFleetUi(engine)
    private val mainCodec=ManufacturingSnapshotCodec(); private val fleetCodec=RobotFleetCodec()
    private var main=loadMain(); private var fleet=loadFleet(); private var selected=fleet.robots.values.first().id; private var message="Automatisation active"; private var lastTick=services.clock.monotonicMillis(); private var layout:RobotFleetUi.Layout?=null
    private val lifecycle=LifecycleObserver{if(it==LifecycleState.BACKGROUND)saveFleet()}
    private val input=object:InputAdapter(){override fun touchDown(x:Int,y:Int,pointer:Int,button:Int):Boolean{touch(ui.unproject(x,y));return true}}
    override fun show(){services.lifecycle.addObserver(lifecycle);Gdx.input.inputProcessor=input;main=loadMain();fleet=engine.normalize(loadFleet(),now());rebalance();transfer(true);lastTick=services.clock.monotonicMillis()}
    override fun hide(){saveFleet();services.lifecycle.removeObserver(lifecycle);if(Gdx.input.inputProcessor===input)Gdx.input.inputProcessor=null}
    override fun resize(w:Int,h:Int)=ui.resize(w,h)
    override fun render(delta:Float){val tick=services.clock.monotonicMillis();if(tick-lastTick>=1_000){transfer(false);lastTick=tick};ScreenUtils.clear(BG);val robot=fleet.robots.getValue(selected);layout=ui.draw(fleet,robot,tasks(robot),message)}
    private fun now()=services.clock.nowEpochMillis().coerceAtLeast(0)
    private fun initialMain()=ManufacturingGameState(economy.initialState(),RefiningState.empty(),AssemblyState.empty())
    private fun loadMain()=services.save.loadLatest()?.let{p->runCatching{require(p.contentVersion==economyDef.contentVersion);mainCodec.decode(p)}.getOrNull()}?:initialMain()
    private fun loadFleet()=services.save.loadLatest(RobotFleetCodec.SLOT_ID)?.let{p->runCatching{require(p.contentVersion==definitions.contentVersion);fleetCodec.decode(p)}.getOrNull()}?:engine.initialState(now())
    private fun saveMain(v:ManufacturingGameState)=services.save.save(mainCodec.encode(v,economyDef.contentVersion,savedAtEpochMillis=now()))==SaveWriteStatus.WRITTEN
    private fun saveFleet(v:RobotAutomationState=fleet)=services.save.save(fleetCodec.encode(v,definitions.contentVersion,now()))==SaveWriteStatus.WRITTEN
    private fun transfer(force:Boolean){val r=engine.advanceLogistics(fleet,economyDef.deposits.values.map{PendingDeposit(it.id,it.resourceId,main.economy.deposits.getValue(it.id).pendingCollection)},main.economy.inventory,economyDef.resources.mapValues{it.value.storageCapacity},economyDef.resources.mapValues{it.value.unitSalePrice},now());if(r.totalMoved==0L){fleet=r.automation;if(force)saveFleet();return};val next=main.copy(economy=main.economy.copy(inventory=r.inventory,deposits=main.economy.deposits.mapValues{(id,s)->s.copy(pendingCollection=r.pendingByDeposit[id]?:s.pendingCollection)},transactionSequence=Math.addExact(main.economy.transactionSequence,1)));if(!saveMain(next)){message="Transfert différé";return};main=next;fleet=r.automation;saveFleet();message="LG-01 : ${r.totalMoved} transférée(s)"}
    private data class Timed(val id:String,val queued:Long,val start:Long,val finish:Long)
    private fun schedule(tasks:List<Timed>,lanes:Int,time:Long):Map<String,Pair<Long,Long>>{val ends=LongArray(lanes){time};return tasks.sortedBy{it.queued}.associate{t->val lane=ends.indices.minBy{ends[it]};val start=maxOf(time,ends[lane]);val finish=Math.addExact(start,(t.finish-t.start).coerceAtLeast(1));ends[lane]=finish;t.id to(start to finish)}}
    private fun rebalance(){val time=now();val rf=schedule(main.refining.jobs.filter{it.status==RefiningJobStatus.QUEUED}.map{Timed(it.id,it.queuedAtEpochMillis,it.startsAtEpochMillis,it.finishesAtEpochMillis)},engine.queueCount(robot(RobotFamily.REFINER)),time);val ass=schedule(main.assembly.jobs.filter{it.status==AssemblyJobStatus.QUEUED}.map{Timed(it.id,it.queuedAtEpochMillis,it.startsAtEpochMillis,it.finishesAtEpochMillis)},engine.queueCount(robot(RobotFamily.ASSEMBLER)),time);if(rf.isEmpty()&&ass.isEmpty())return;val next=main.copy(refining=main.refining.copy(jobs=main.refining.jobs.map{rf[it.id]?.let{t->it.copy(startsAtEpochMillis=t.first,finishesAtEpochMillis=t.second)}?:it}),assembly=main.assembly.copy(jobs=main.assembly.jobs.map{ass[it.id]?.let{t->it.copy(startsAtEpochMillis=t.first,finishesAtEpochMillis=t.second)}?:it}));if(next!=main&&saveMain(next))main=next}
    private fun tasks(r:RobotInstance):List<QueueTask> = when(r.family){RobotFamily.REFINER->main.refining.jobs.take(6).map{QueueTask(it.id,((it.finishesAtEpochMillis-it.startsAtEpochMillis)/1_000).coerceAtLeast(1))};RobotFamily.ASSEMBLER->main.assembly.jobs.take(6).map{QueueTask(it.id,((it.finishesAtEpochMillis-it.startsAtEpochMillis)/1_000).coerceAtLeast(1))};else->(1..6).map{QueueTask("${r.family}_$it",7L+it*4)}}
    private fun touch(p:com.badlogic.gdx.math.Vector2){val l=layout?:return;l.cards.forEachIndexed{i,r->if(r.contains(p)){selected=ordered()[i].id;services.haptic.impact();return}};when{l.priority.contains(p)->priority();l.upgrade.contains(p)->upgrade();l.quality.contains(p)->{fleet=engine.cycleQuality(fleet);saveFleet();message="${engine.visibleUnitCount(fleet)} unités"};l.back.contains(p)->onBack()}}
    private fun priority()=when(val r=engine.cyclePriority(fleet,selected)){is RobotCommandResult.Applied->{fleet=r.state;saveFleet();message=fleet.robots.getValue(selected).priority.name;services.haptic.success()};is RobotCommandResult.Rejected->services.haptic.warning()}
    private fun upgrade(){val oldMain=main;val oldFleet=fleet;when(val r=engine.upgrade(fleet,selected,main.economy.spaceDollars)){is RobotCommandResult.Rejected->{message=if(r.code=="insufficient_space_dollars")"SpaceDollars insuffisants" else "Niveau maximal";services.haptic.warning()};is RobotCommandResult.Applied->{val next=main.copy(economy=main.economy.copy(spaceDollars=main.economy.spaceDollars-r.transaction.spaceDollarCost,transactionSequence=Math.addExact(main.economy.transactionSequence,1)));if(!saveMain(next)){message="Amélioration annulée";return};main=next;fleet=r.state;rebalance();if(!saveFleet()){saveMain(oldMain);main=oldMain;fleet=oldFleet;message="Amélioration annulée";return};message="Niveau ${fleet.robots.getValue(selected).level}";services.haptic.success()}}}
    private fun robot(f:RobotFamily)=fleet.robots.values.first{it.family==f};private fun ordered()=fleet.robots.values.sortedBy{it.family.ordinal}
    override fun dispose(){hide();ui.dispose()}
    private companion object{val BG=Color(.008f,.014f,.03f,1f)}
}
