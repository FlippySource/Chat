package example.mediarenderer;

import org.teleal.cling.support.avtransport.impl.AVTransportStateMachine;
import org.teleal.common.statemachine.States;

@States({
        MyRendererNoMediaPresent.class,
        MyRendererStopped.class,
        MyRendererPlaying.class
})
interface MyRendererStateMachine extends AVTransportStateMachine {}
