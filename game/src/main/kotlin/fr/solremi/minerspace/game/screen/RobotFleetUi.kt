package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.domain.robot.*
import kotlin.math.max

internal class RobotFleetUi(private val engine: RobotAutomationEngine) {
    val camera = OrthographicCamera()
    val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer(); private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.68f) }; private val small = BitmapFont().apply { data.setScale(.54f) }
    private val dronePoints = List(50) { Vector2((it % 10) / 9f, (it / 10) / 4f) }

    data class Layout(val cards: List<Rectangle>, val priority: Rectangle, val upgrade: Rectangle, val quality: Rectangle, val back: Rectangle)

    fun draw(fleet: RobotAutomationState, selected: RobotInstance, tasks: List<QueueTask>, message: String): Layout {
        viewport.apply(); camera.update(); val l = fullLayout(); val ordered = fleet.robots.values.sortedBy { it.family.ordinal }
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP; shapes.rect(l.top.x, l.top.y, l.top.width, l.top.height)
        drawDrones(l.detail, engine.visibleUnitCount(fleet)); ordered.forEachIndexed { i, r -> card(l.cards[i], r, r.id == selected.id) }
        shapes.color = PANEL; shapes.rect(l.detail.x, l.detail.y, l.detail.width, l.detail.height)
        robotShape(l.visual, selected); queueBars(l.queue, selected, tasks)
        listOf(l.priority, l.upgrade, l.quality, l.back).forEach(::button); shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin(); font.color = TEXT
        font.draw(batch, "FLOTTE AUTOMATISÉE", l.top.x + 12f, l.top.y + 33f); small.color = MUTED
        small.draw(batch, "${engine.visibleUnitCount(fleet)} unités visibles · ${fleet.renderQuality.name}", l.top.x + 12f, l.top.y + 13f)
        ordered.forEachIndexed { i, r -> cardText(l.cards[i], r) }; details(l.detail, selected, message)
        label(l.priority, "PRIORITÉ"); label(l.upgrade, "AMÉLIORER"); label(l.quality, "QUALITÉ"); label(l.back, "RETOUR"); batch.end()
        return Layout(l.cards, l.priority, l.upgrade, l.quality, l.back)
    }

    private fun drawDrones(area: Rectangle, count: Int) { dronePoints.take(count).forEachIndexed { i, p -> shapes.color = if (i % 7 == 0) DRONE_ACTIVE else DRONE; shapes.circle(area.x + 12 + p.x * (area.width - 24), area.y + 12 + p.y * (area.height - 24), if (i < 4) 3.5f else 2.2f, 8) } }
    private fun card(r: Rectangle, robot: RobotInstance, active: Boolean) { shapes.color = if (active) CARD_ACTIVE else CARD; shapes.rect(r.x,r.y,r.width,r.height); shapes.color = familyColor(robot.family); shapes.rect(r.x,r.y,6f,r.height) }
    private fun cardText(r: Rectangle, robot: RobotInstance) { font.color=TEXT; font.draw(batch,"${robot.displayName} · ${code(robot.family)}",r.x+12,r.y+29); small.color=MUTED; small.draw(batch,"${robot.serialNumber} · N${robot.level} · ${engine.queueCount(robot)} file(s)",r.x+12,r.y+11) }
    private fun robotShape(r: Rectangle, robot: RobotInstance) { val tier=engine.visualTier(robot); val w=42f+tier*10; val h=34f+tier*7; val x=r.x+(r.width-w)/2; val y=r.y+14; shapes.color=familyColor(robot.family); shapes.rect(x,y,w,h); shapes.color=WINDOW; shapes.rect(x+9,y+h-16,w-18,9f); repeat(tier){shapes.color=DETAIL;shapes.rect(x+6+it*13,y+h,8f,6f+it*3)} }
    private fun queueBars(r: Rectangle, robot: RobotInstance, tasks: List<QueueTask>) { val lanes=engine.queueCount(robot); val plan=engine.planQueues(robot,tasks.ifEmpty{listOf(QueueTask("auto_1",12),QueueTask("auto_2",18),QueueTask("auto_3",10))}); val maxEnd=plan.maxOfOrNull{it.finishesAtSecond}?.coerceAtLeast(1)?:1; val h=r.height/lanes; repeat(lanes){shapes.color=GRID;shapes.rect(r.x,r.y+it*h,r.width,h-3)}; plan.forEach{a->shapes.color=if(a.laneIndex%2==0)QUEUE_A else QUEUE_B;shapes.rect(r.x+a.startsAtSecond.toFloat()/maxEnd*r.width,r.y+a.laneIndex*h+3,((a.finishesAtSecond-a.startsAtSecond).toFloat()/maxEnd*r.width).coerceAtLeast(3f),h-9)} }
    private fun details(r: Rectangle, robot: RobotInstance, message: String) { font.color=TEXT;font.draw(batch,"${robot.displayName} · ${robot.serialNumber}",r.x+12,r.y+r.height-14);small.color=MUTED;small.draw(batch,"${family(robot.family)} · niveau ${robot.level}/5 · visuel ${engine.visualTier(robot)}/3",r.x+12,r.y+r.height-34);small.draw(batch,"Trait ${trait(robot.trait)} · maîtrise ${mastery(engine.masteryTier(robot))} (${robot.masteryPoints})",r.x+12,r.y+r.height-52);small.draw(batch,"Priorité ${priority(robot.priority)} · ${engine.queueCount(robot)} file(s)",r.x+12,r.y+r.height-70);small.draw(batch,"Travail ${robot.statistics.workFor(robot.family)} · ${robot.statistics.activeSeconds}s actif",r.x+12,r.y+13);small.draw(batch,"${engine.upgradeCost(robot)?.let{"Prochain niveau $it SD"}?:"Niveau maximal"} · $message",r.x+12,r.y+31) }
    private fun button(r:Rectangle){shapes.color=BUTTON;shapes.rect(r.x,r.y,r.width,r.height);shapes.color=ACCENT;shapes.rect(r.x,r.y,r.width,4f)}
    private fun label(r:Rectangle,text:String){small.color=TEXT;small.draw(batch,text,r.x+7,r.y+29)}

    private data class FullLayout(val top:Rectangle,val cards:List<Rectangle>,val detail:Rectangle,val visual:Rectangle,val queue:Rectangle,val priority:Rectangle,val upgrade:Rectangle,val quality:Rectangle,val back:Rectangle)
    private fun fullLayout():FullLayout{val s=safe();val top=Rectangle(s.l,s.t-50,s.r-s.l,50f);val bottom=s.b;val cb=bottom+54;val ct=top.y-6;val h=(ct-cb).coerceAtLeast(200f);val lw=minOf(190f,(s.r-s.l)*.34f);val ch=(h-12)/4;val cards=List(4){i->Rectangle(s.l,ct-(i+1)*ch-i*4,lw,ch)};val d=Rectangle(s.l+lw+6,cb,s.r-s.l-lw-6,h);val v=Rectangle(d.x+d.width-120,d.y+46,110f,d.height-56);val q=Rectangle(d.x+12,d.y+50,(d.width-150).coerceAtLeast(125f),46f);val back=Rectangle(s.r-94,bottom,94f,48f);val quality=Rectangle(back.x-100,bottom,94f,48f);val upgrade=Rectangle(quality.x-108,bottom,102f,48f);val priority=Rectangle(upgrade.x-102,bottom,96f,48f);return FullLayout(top,cards,d,v,q,priority,upgrade,quality,back)}
    private data class Safe(val l:Float,val r:Float,val b:Float,val t:Float)
    private fun safe():Safe{val w=viewport.worldWidth;val h=viewport.worldHeight;val sx=w/Gdx.graphics.width.coerceAtLeast(1);val sy=h/Gdx.graphics.height.coerceAtLeast(1);val l=Gdx.graphics.safeInsetLeft*sx+8;val r=max(l+1,w-Gdx.graphics.safeInsetRight*sx-8);val b=Gdx.graphics.safeInsetBottom*sy+8;val t=max(b+1,h-Gdx.graphics.safeInsetTop*sy-8);return Safe(l,r,b,t)}
    fun resize(w:Int,h:Int)=viewport.update(w.coerceAtLeast(1),h.coerceAtLeast(1),true)
    fun unproject(x:Int,y:Int)=viewport.unproject(Vector2(x.toFloat(),y.toFloat()))
    fun dispose(){shapes.dispose();batch.dispose();font.dispose();small.dispose()}
    private fun code(f:RobotFamily)=listOf("EX-01","RF-01","AS-01","LG-01")[f.ordinal]
    private fun family(f:RobotFamily)=listOf("Extracteur","Raffineur","Assembleur","Logistique")[f.ordinal]
    private fun familyColor(f:RobotFamily)=listOf(EX,RF,AS,LG)[f.ordinal]
    private fun trait(t:RobotTrait)=listOf("Précis +10%","Endurant +15%","Rapide +20%","Stable +8%","Prospecteur +5%")[t.ordinal]
    private fun mastery(t:MasteryTier)=listOf("Novice","Expérimenté","Expert","Vétéran")[t.ordinal]
    private fun priority(p:AutomationPriority)=listOf("Équilibrée","Mission","Désengorger","Ressource rare","Valeur")[p.ordinal]
    private companion object{val TOP=Color(.025f,.055f,.095f,1f);val PANEL=Color(.03f,.065f,.11f,.96f);val CARD=Color(.045f,.08f,.12f,.96f);val CARD_ACTIVE=Color(.075f,.15f,.20f,1f);val GRID=Color(.12f,.18f,.23f,1f);val BUTTON=Color(.075f,.17f,.24f,1f);val ACCENT=Color(.20f,.82f,.88f,1f);val TEXT=Color(.92f,.97f,1f,1f);val MUTED=Color(.62f,.73f,.82f,1f);val EX=Color(.76f,.43f,.18f,1f);val RF=Color(.25f,.58f,.82f,1f);val AS=Color(.58f,.38f,.82f,1f);val LG=Color(.22f,.72f,.48f,1f);val WINDOW=Color(.72f,.92f,1f,1f);val DETAIL=Color(.86f,.70f,.22f,1f);val DRONE=Color(.12f,.23f,.28f,.55f);val DRONE_ACTIVE=Color(.22f,.66f,.72f,.75f);val QUEUE_A=Color(.20f,.66f,.72f,.9f);val QUEUE_B=Color(.54f,.38f,.75f,.9f)}
}
