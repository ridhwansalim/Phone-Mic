package com.phonemic.app;
import java.util.*;
public class EchoDriftCheck {
    public static void main(String[] args)throws Exception {
        for(int ppm:new int[]{0,300,1000}) {
            short[] history=new short[131072],frame=new short[480];long cursor=0;
            try(SpeakerEcho echo=new SpeakerEcho()) {
                for(int n=0;n<1800;n++) {
                    for(int i=0;i<480;i++) {
                        double pos=(cursor+i)*(1-ppm/1000000.0)-35376;
                        long index=(long)Math.floor(pos);double part=pos-index;
                        frame[i]=index<0?0:(short)(.8*((1-part)*history[(int)index & 131071]+part*history[(int)(index+1)&131071]));
                    }
                    echo.capture(frame);
                    if(echo.ready()){System.out.println(ppm+" ppm: passed");break;}
                    for(int i=0;i<480;i++)history[(int)(cursor+i)&131071]=frame[i];
                    echo.rendered(frame,0,480);cursor+=480;
                    if(n>=SpeakerEcho.CALIBRATION_FRAMES)Thread.sleep(5);
                }
            }catch(SpeakerEcho.CalibrationFailure e){System.out.println(ppm+" ppm: "+e.getMessage());}
        }
    }
}
