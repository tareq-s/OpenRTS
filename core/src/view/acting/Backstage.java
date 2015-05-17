package view.acting;

import geometry.tools.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import model.battlefield.actors.Actor;
import model.battlefield.actors.ActorPool;
import view.material.MaterialManager;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.effect.ParticleEmitter;
import com.jme3.material.Material;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author Benoît
 */
public class Backstage implements AnimEventListener {

    AssetManager assetManager;
    public MaterialManager materialManager;
    
    ActorPool pool;
    public Node mainNode;
    public PhysicsSpace mainPhysicsSpace;
    
    ModelPerformer modelPfm;
    ParticlePerformer particlePfm;
    AnimationPerformer animationPfm;
    RagdollPerformer physicPfm;
    SoundPerformer soundPfm;
    
    
    HashMap<String, Spatial> models = new HashMap<>();
    HashMap<String, AudioNode> sounds = new HashMap<>();
    
    List<ParticleEmitter> dyingEmitters = new ArrayList<>();
    List<PhysicsRigidBody> pausedPhysics = new ArrayList<>();

    public Backstage(AssetManager assetManager, MaterialManager materialManager, ActorPool pool){
        this.assetManager = assetManager;
        this.materialManager = materialManager;
        this.pool = pool;
        mainNode = new Node();
        
        modelPfm = new ModelPerformer(this);
        particlePfm = new ParticlePerformer(this);
        animationPfm = new AnimationPerformer(this);
        physicPfm = new RagdollPerformer(this);
        soundPfm = new SoundPerformer(this);
   }
    
    public void render(){
        // first, the spatials attached to interrupted actor are detached
        for(Actor a : pool.grabDeletedActors()){
            if(a.viewElements.spatial != null) {
				mainNode.detachChild(a.viewElements.spatial);
			}
            if(a.viewElements.particleEmitter != null){
            	a.viewElements.particleEmitter.setParticlesPerSec(0);
                dyingEmitters.add(a.viewElements.particleEmitter);
                a.viewElements.particleEmitter = null;                
            }
            if(a.viewElements.selectionCircle != null) {
				mainNode.detachChild(a.viewElements.selectionCircle);
			}
        }
        List<ParticleEmitter> deleted = new ArrayList<>();
    	for(ParticleEmitter pe : dyingEmitters) {
			if(pe.getNumVisibleParticles() == 0){
    			mainNode.detachChild(pe);
    			deleted.add(pe);
    		}
		}
    	dyingEmitters.removeAll(deleted);
        
    	
    	
        for(Actor a : pool.getActors()) {
			switch (a.getType()){
            	case "model" : modelPfm.perform(a); break;
            }
		}

        for(Actor a : pool.getActors()) {
			switch (a.getType()){
                case "physic" : physicPfm.perform(a); break;
                case "animation" : animationPfm.perform(a); break;
                case "particle" : particlePfm.perform(a); break;
                case "sound" : soundPfm.perform(a); break;
            }
		}
    }
    
    public void pause(boolean val){
    	setEmmitersEnable(mainNode, val);
    	if(val){
    		pausedPhysics.clear();
	    	Iterator<PhysicsRigidBody> i = mainPhysicsSpace.getRigidBodyList().iterator();
	    	while(i.hasNext()){
	    		PhysicsRigidBody rb = i.next();
	    		mainPhysicsSpace.remove(rb);
	    		pausedPhysics.add(rb);
	    	}
    	} else {
    		for(PhysicsRigidBody rb : pausedPhysics) {
				mainPhysicsSpace.add(rb);
			}
    	}
    	
	}

    public void setEmmitersEnable(Spatial s, boolean val){
    	if(s instanceof ParticleEmitter) {
			((ParticleEmitter)s).setEnabled(!val);
		}
    	if(s instanceof Node) {
			for(Spatial child : ((Node)s).getChildren()) {
				setEmmitersEnable(child, val);
			}
		}
    }
    
    
    protected Spatial buildSpatial(String modelPath){
        if(!models.containsKey(modelPath)) {
			models.put(modelPath, assetManager.loadModel("models/"+modelPath));
		}
        Spatial res = models.get(modelPath).clone();
        if(res == null) {
			LogUtil.logger.info(modelPath);
		}
        AnimControl control = res.getControl(AnimControl.class);
        if(control != null){
            control.addListener(this);
            control.createChannel();
        }
        mainNode.attachChild(res);
        return res;
    }
    
    protected Material getParticleMat(String texturePath){
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        m.setTexture("Texture", assetManager.loadTexture("textures/"+texturePath));
        return m;
    }
    
    protected AudioNode getAudioNode(String soundPath){
        if(!sounds.containsKey(soundPath)){
            sounds.put(soundPath, new AudioNode(assetManager, "sounds/"+soundPath));
    		mainNode.attachChild(sounds.get(soundPath));
        }
        return sounds.get(soundPath);
    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
    }

    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
//    	LogUtil.logger.info("anim changed to "+animName);
    }

}
