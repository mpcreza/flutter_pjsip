package com.jvtd.flutter_pjsip;

import android.util.Log;

import com.jvtd.flutter_pjsip.entity.MyAccount;
import com.jvtd.flutter_pjsip.entity.MyCall;
import com.jvtd.flutter_pjsip.interfaces.MyAppObserver;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IpChangeParam;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjmedia_srtp_use;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsip_transport_type_e;
import org.pjsip.pjsua2.pjsua_stun_use;

import static org.pjsip.pjsua2.pj_constants_.PJ_TRUE;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_REINIT_MEDIA;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_VIA;

/**
 * Description: PjSip管理类
 * Author: Jack Zhang
 * create on: 2019-08-21 23:19
 */
public class PjSipManager
{
  private static volatile PjSipManager mInstance;
  private AccountConfig mAccountConfig;
  private MyAccount mAccount;
  SipLogWriter logWriter;

  static
  {
    try
    {
      System.loadLibrary("openh264");
      // Ticket #1937: libyuv is now included as static lib
      //System.loadLibrary("yuv");
    } catch (UnsatisfiedLinkError e)
    {
      System.out.println("UnsatisfiedLinkError: " + e.getMessage());
      System.out.println("This could be safely ignored if you don't need video.");
    }
    try
    {
      System.loadLibrary("pjsua2");
      System.out.println("Library loaded");
    } catch (Exception e)
    {
      e.printStackTrace();
    } catch (Error error)
    {
      error.printStackTrace();
    }
  }

  public static PjSipManager getInstance()
  {
    if (mInstance == null)
      synchronized (PjSipManager.class)
      {
        if (mInstance == null)
          mInstance = new PjSipManager();
      }
    return mInstance;
  }

  private PjSipManager()
  {

  }

  public static Endpoint mEndPoint;
  public static MyAppObserver observer;

  /**
   * 初始化方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 14:34
   */
  public void init(MyAppObserver obs, boolean enableLog)
  {
    init(obs, false, enableLog);
  }

  /**
   * 初始化方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 14:34
   */
  public void init(MyAppObserver obs, boolean own_worker_thread, boolean enableLog)
  {
    observer = obs;

    /* Create endpoint */
    try
    {
      if (mEndPoint == null)
        mEndPoint = new Endpoint();
      mEndPoint.libCreate();
    } catch (Exception e)
    {
      return;
    }

    EpConfig epConfig = new EpConfig();

    if(enableLog) {
      LogConfig logConfig = epConfig.getLogConfig();
      logConfig.setLevel(4);
      logConfig.setConsoleLevel(5);
      logWriter = new SipLogWriter();
      logConfig.setWriter(logWriter);
      logConfig.setDecor(logConfig.getDecor() & ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() | pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()));
    }


    // UAConfig，指定核心SIP用户代理设置
    UaConfig ua_cfg = epConfig.getUaConfig();
    ua_cfg.setUserAgent("Pjsua2 Android " + mEndPoint.libVersion().getFull());

    /* STUN server. */
    //StringVector stun_servers = new StringVector();
    //stun_servers.add("stun.pjsip.org");
    //ua_cfg.setStunServer(stun_servers);

    /* No worker thread */
    if (own_worker_thread)
    {
//      ua_cfg.setThreadCnt(0);
      ua_cfg.setMainThreadOnly(true);
    }

    // 指定ep_cfg中设置的自定义
    try
    {
      mEndPoint.libInit(epConfig);
    } catch (Exception e)
    {
      e.printStackTrace();
      return;
    }

    TransportConfig sipTpConfig = new TransportConfig();
    int SIP_PORT = 6050;

    /* Set SIP port back to default for JSON saved config */
//    sipTpConfig.setPort(SIP_PORT);

    // 创建一个或多个传输
//    try
//    {
//      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);
//    } catch (Exception e)
//    {
//      e.printStackTrace();
//      return;
//    }
//    try
//    {
//      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);
//    } catch (Exception e)
//    {
//      e.printStackTrace();
//    }
//
    try
    {
//      sipTpConfig.setPort(SIP_PORT + 1);
      mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, sipTpConfig);
    } catch (Exception e)
    {
      e.printStackTrace();
    }

    /* Start. */
    try
    {
      mEndPoint.libStart();
      mEndPoint.codecSetPriority("opus/48000/2", (short) 255);
    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void handleNetworkChange()
  {
    try
    {
      System.out.println("Network change detected");
      IpChangeParam changeParam = new IpChangeParam();
      mEndPoint.handleIpChange(changeParam);
    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void deinit()
  {
    /* Try force GC to avoid late destroy of PJ objects as they should be
     * deleted before lib is destroyed.
     */
    Runtime.getRuntime().gc();

    /* Shutdown pjsua. Note that Endpoint destructor will also invoke
     * libDestroy(), so this will be a test of double libDestroy().
     */
    try
    {
      mEndPoint.libDestroy();
    } catch (Exception e)
    {
      e.printStackTrace();
    }

    /* Force delete Endpoint here, to avoid deletion from a non-
     * registered thread (by GC?).
     */
    mEndPoint.delete();
    mEndPoint = null;
  }

  public void login(String username, String password, String ip, String port)
  {
    String transportString = ";transport=tls";
    String sipAccountRegId = "sip:" + username + "@" + ip + ":" + port + transportString;
    String sipRegistrarUri = "sip:" + ip + ":" + port + transportString;

    mAccountConfig = new AccountConfig();
//    mAccountConfig.getNatConfig().setIceEnabled(false);
    // 未实现视频功能，先置位false
    mAccountConfig.getVideoConfig().setAutoTransmitOutgoing(false);// 自动向外传输视频流
    mAccountConfig.getVideoConfig().setAutoShowIncoming(false);// 自动接收并显示来的视频流
    mAccountConfig.setIdUri(sipAccountRegId);
    mAccountConfig.getRegConfig().setRegistrarUri(sipRegistrarUri);
    mAccountConfig.getSipConfig().getProxies().add(sipRegistrarUri);
//    mAccountConfig.getNatConfig().setSipStunUse(pjsua_stun_use.PJSUA_STUN_USE_DISABLED);

    // TLS Configuration
//    mAccountConfig.getMediaConfig().setSrtpSecureSignaling(1);
//    mAccountConfig.getMediaConfig().setSrtpUse(pjmedia_srtp_use.PJMEDIA_SRTP_MANDATORY);

    // IP Address account configuration
    mAccountConfig.getIpChangeConfig().setShutdownTp(false);
    mAccountConfig.getIpChangeConfig().setHangupCalls(false);
    mAccountConfig.getIpChangeConfig().setReinviteFlags(PJSUA_CALL_UPDATE_CONTACT.swigValue() | PJSUA_CALL_REINIT_MEDIA.swigValue() | PJSUA_CALL_UPDATE_VIA.swigValue());

    mAccountConfig.getNatConfig().setContactRewriteUse(PJ_TRUE.swigValue());
    mAccountConfig.getNatConfig().setContactRewriteMethod(4);

    AuthCredInfoVector creds = mAccountConfig.getSipConfig().getAuthCreds();
    if (creds != null)
    {
      creds.clear();
      if (username != null && username.length() != 0)
        creds.add(new AuthCredInfo("Digest", "*", username, 0, password));
    }

    mAccount = new MyAccount(mAccountConfig);
    try
    {
      mAccount.create(mAccountConfig);
    } catch (Exception e)
    {
      e.printStackTrace();
      mAccount = null;
    }
  }

  public MyCall call(String username, String ip, String port)
  {
    MyCall call = new MyCall(mAccount, -1);
    CallOpParam prm = new CallOpParam();
    prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
//    prm.getOpt().setAudioCount(1);
//    prm.getOpt().setVideoCount(1);
    String uri = "sip:" + username + "@" + ip + ":" + port;
    try
    {
      call.makeCall(uri, prm);
    } catch (Exception e)
    {
      call.delete();
      return null;
    }
    return call;
  }

  public void logout()
  {
    mAccountConfig.delete();
    mAccount.delete();
  }
}
