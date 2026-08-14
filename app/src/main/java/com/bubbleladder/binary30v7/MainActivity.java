package com.bubbleladder.binary30v7;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity{
    private static final int REQ_EXPORT=9401,REQ_IMPORT=9402,REQ_NOTI=9403;
    private final Handler h=new Handler(Looper.getMainLooper());
    private final ExecutorService ex=Executors.newSingleThreadExecutor();

    private TextView countdown,bgState,status,nextRound,triple,exclude,grade,flow,patterns,backtest,live,profit,recent;
    private EditText stake,odds;
    private CheckBox background;
    private Button refresh,saveSetting,backup,restore,reset;

    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){reloadAsync();}
    };
    private final Runnable countdownTask=new Runnable(){
        @Override public void run(){
            if(countdown!=null)countdown.setText(FlowCore.countdownText());
            h.postDelayed(this,1000L);
        }
    };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(buildUi());
        loadSettings();
        bindActions();
        registerUpdates();
        requestNotificationPermissionIfNeeded();
        h.post(countdownTask);
        if(FlowCore.prefs(this).getBoolean(FlowCore.K_AUTO,true))startAutoService();
        reloadAsync();
    }

    private View buildUi(){
        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(16),dp(14),dp(30));
        root.setBackgroundColor(Color.rgb(7,19,26));
        sv.addView(root);

        root.addView(tv("보글사다리3 · V7 Binary30 Snapshot",24,Color.WHITE,true));
        TextView sub=tv("API 최신30회만 · 이진 3차원 · 최근3~6 완전패턴 · 누적 0",12,Color.rgb(110,231,183),false);
        sub.setPadding(0,dp(4),0,dp(14));
        root.addView(sub);

        LinearLayout clock=card();
        clock.addView(tv("다음 추첨까지",12,Color.rgb(148,163,184),false));
        countdown=tv("--:--",38,Color.rgb(56,189,248),true);
        clock.addView(countdown);
        bgState=tv("백그라운드 상태 확인 중",12,Color.rgb(203,213,225),false);
        bgState.setPadding(0,dp(4),0,0);
        clock.addView(bgState);
        root.addView(clock);

        LinearLayout ctrl=card();
        refresh=button("🔄 지금 추첨 / API30 Binary 분석",Color.rgb(5,150,105));
        ctrl.addView(refresh,new LinearLayout.LayoutParams(-1,dp(54)));
        background=new CheckBox(this);
        background.setText("백그라운드 자동추첨 ON");
        background.setTextColor(Color.WHITE);
        background.setTextSize(15);
        background.setPadding(0,dp(8),0,0);
        ctrl.addView(background);
        status=tv("조회 준비",12,Color.rgb(203,213,225),false);
        status.setPadding(0,dp(6),0,0);
        ctrl.addView(status);
        root.addView(ctrl);

        LinearLayout hero=card();
        hero.addView(tv("다음 회차 V7 Binary30 삼치기",12,Color.GRAY,false));
        nextRound=tv("-",15,Color.WHITE,true);
        hero.addView(nextRound);
        triple=tv("분석 대기",30,Color.rgb(52,211,153),true);
        triple.setPadding(0,dp(7),0,dp(5));
        hero.addView(triple);
        exclude=tv("제외조합 -",17,Color.rgb(248,113,113),true);
        hero.addView(exclude);
        grade=tv("흐름 신호 -",15,Color.rgb(253,224,71),true);
        grade.setPadding(0,dp(5),0,0);
        hero.addView(grade);
        TextView note=tv("● 매번 API 최신30회로 교체 · 31회 이상 누적하지 않음",12,Color.rgb(110,231,183),true);
        note.setPadding(0,dp(10),0,0);
        hero.addView(note);
        root.addView(hero);

        LinearLayout fc=card();
        fc.addView(section("좌우 / 사다리수 / 홀짝 · API30 이진분석"));
        flow=tv("-",14,Color.WHITE,false);
        flow.setLineSpacing(0,1.3f);
        fc.addView(flow);
        root.addView(fc);

        LinearLayout pc=card();
        pc.addView(section("3차원별 최근3·4·5·6 완전 동일패턴"));
        patterns=tv("-",14,Color.rgb(226,232,240),false);
        patterns.setLineSpacing(0,1.3f);
        pc.addView(patterns);
        root.addView(pc);

        LinearLayout bc=card();
        bc.addView(section("현재 API30 내부 · 미래누설 없는 순차 재현"));
        backtest=tv("-",14,Color.WHITE,false);
        backtest.setLineSpacing(0,1.3f);
        bc.addView(backtest);
        live=tv("V7 실전 기록 · 아직 없음",14,Color.rgb(125,211,252),true);
        live.setPadding(0,dp(10),0,0);
        bc.addView(live);
        root.addView(bc);

        LinearLayout bet=card();
        bet.addView(section("고정배팅 · 수익 계산"));
        LinearLayout ir=new LinearLayout(this);
        ir.setOrientation(LinearLayout.HORIZONTAL);
        stake=input("5000");
        stake.setHint("1개 배팅금액");
        stake.setInputType(InputType.TYPE_CLASS_NUMBER);
        odds=input("1.95");
        odds.setHint("배당");
        odds.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ir.addView(stake,new LinearLayout.LayoutParams(0,dp(52),1));
        LinearLayout.LayoutParams olp=new LinearLayout.LayoutParams(0,dp(52),1);
        olp.setMargins(dp(8),0,0,0);
        ir.addView(odds,olp);
        bet.addView(ir);
        saveSetting=button("설정 저장 · 최소 5,000원",Color.rgb(30,64,175));
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(48));
        slp.setMargins(0,dp(8),0,0);
        bet.addView(saveSetting,slp);
        profit=tv("-",13,Color.rgb(226,232,240),false);
        profit.setPadding(0,dp(10),0,0);
        bet.addView(profit);
        root.addView(bet);

        LinearLayout rc=card();
        rc.addView(section("최근 15회"));
        recent=tv("-",14,Color.WHITE,false);
        recent.setLineSpacing(0,1.25f);
        rc.addView(recent);
        root.addView(rc);

        LinearLayout dc=card();
        dc.addView(section("데이터 백업 / 기존 분석기 결과 가져오기"));
        LinearLayout dr=new LinearLayout(this);
        dr.setOrientation(LinearLayout.HORIZONTAL);
        backup=button("💾 V7 백업",Color.rgb(21,128,61));
        restore=button("📂 복원/가져오기",Color.rgb(109,40,217));
        dr.addView(backup,new LinearLayout.LayoutParams(0,dp(50),1));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(50),1);
        rp.setMargins(dp(8),0,0,0);
        dr.addView(restore,rp);
        dc.addView(dr);
        reset=button("V7 성적만 초기화",Color.rgb(127,29,29));
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,dp(46));
        rlp.setMargins(0,dp(8),0,0);
        dc.addView(reset,rlp);
        root.addView(dc);

        root.addView(tv("※ V7은 과거 누적 history를 쓰지 않습니다. 매 회차 API가 주는 최신30회만 이진 분해해 분석하며, 무작위라면 삼치기 기준 성공률은 75%입니다.",11,Color.GRAY,false));
        return sv;
    }

    private void bindActions(){
        refresh.setOnClickListener(v->manualSync());
        saveSetting.setOnClickListener(v->saveSettings());
        background.setOnCheckedChangeListener((v,on)->{
            FlowCore.prefs(this).edit().putBoolean(FlowCore.K_AUTO,on).apply();
            if(on)startAutoService();else stopAutoService();
            updateBgState();
        });
        backup.setOnClickListener(v->startExport());
        restore.setOnClickListener(v->startImport());
        reset.setOnClickListener(v->confirmReset());
    }

    private void loadSettings(){
        android.content.SharedPreferences sp=FlowCore.prefs(this);
        stake.setText(String.valueOf(Math.max(5000,sp.getInt(FlowCore.K_BASE_STAKE,5000))));
        odds.setText(String.valueOf(sp.getFloat(FlowCore.K_ODDS,1.95f)));
        background.setChecked(sp.getBoolean(FlowCore.K_AUTO,true));
        updateBgState();
    }

    private int readStake(){
        try{return Math.max(5000,Integer.parseInt(stake.getText().toString().trim()));}
        catch(Exception e){return 5000;}
    }

    private double readOdds(){
        try{return Math.max(1.01,Double.parseDouble(odds.getText().toString().trim()));}
        catch(Exception e){return 1.95;}
    }

    private void saveSettings(){
        int s=readStake();
        double o=readOdds();
        FlowCore.prefs(this).edit().putInt(FlowCore.K_BASE_STAKE,s).putFloat(FlowCore.K_ODDS,(float)o).apply();
        stake.setText(String.valueOf(s));
        Toast.makeText(this,"설정 저장 완료",Toast.LENGTH_SHORT).show();
        reloadAsync();
    }

    private void saveSettingsSilent(){
        FlowCore.prefs(this).edit().putInt(FlowCore.K_BASE_STAKE,readStake()).putFloat(FlowCore.K_ODDS,(float)readOdds()).apply();
    }

    private void manualSync(){
        saveSettingsSilent();
        refresh.setEnabled(false);
        status.setText("API 최신30회 이진분석 중...");
        ex.execute(()->{
            try{
                FlowCore.SyncResult sr=FlowCore.sync(this);
                h.post(()->{
                    render(sr.analysis,sr.history);
                    status.setText("● 완료 · "+new SimpleDateFormat("HH:mm:ss",Locale.KOREA).format(new Date()));
                    status.setTextColor(Color.rgb(52,211,153));
                    refresh.setEnabled(true);
                });
            }catch(Exception e){
                h.post(()->{
                    status.setText("조회 실패: "+e.getMessage());
                    status.setTextColor(Color.rgb(248,113,113));
                    refresh.setEnabled(true);
                });
            }
        });
    }

    private void reloadAsync(){
        ex.execute(()->{
            List<FlowCore.Result>d=FlowCore.load(this);
            FlowCore.Analysis a=d.isEmpty()?null:FlowCore.analyze(d);
            h.post(()->{
                if(a!=null)render(a,d);
                else status.setText("데이터 없음 · 지금 추첨/분석 실행을 눌러주세요.");
                updateBgState();
            });
        });
    }

    private void render(FlowCore.Analysis a,List<FlowCore.Result>d){
        if(a==null||d==null||d.isEmpty())return;
        FlowCore.Result last=d.get(0);
        nextRound.setText(last.round<480?last.date+" · "+(last.round+1)+"회":"다음날 · 1회");
        triple.setText(a.triple);
        exclude.setText("최종 제외: "+FlowCore.COMBO[a.exclude]+"\n선택 3개: "+a.selected);
        grade.setText(gradeIcon(a.grade)+" "+a.grade+" · "+a.mode);

        StringBuilder f=new StringBuilder();
        f.append("API 분석 데이터: ").append(a.count30).append("/30회")
                .append("\n범위: ").append(a.windowRange)
                .append("\n현재 마지막 조합흐름: ").append(a.suffix)
                .append("\n\n[조합별 반대표]")
                .append("\n좌3짝 ").append(a.oppose[1])
                .append(" · 좌4홀 ").append(a.oppose[2])
                .append(" · 우3홀 ").append(a.oppose[3])
                .append(" · 우4짝 ").append(a.oppose[4])
                .append("\n최다 ").append(a.topOppose)
                .append(" · 2위 ").append(a.secondOppose)
                .append(" · 결정표 ").append(a.decisiveVotes).append("/12")
                .append("\n\n과거 누적 없음 · Rolling480 없음 · 가중치 없음")
                .append("\n매 조회마다 API 최신30회로 완전 교체");
        flow.setText(f.toString());

        StringBuilder ps=new StringBuilder();
        for(int di=0;di<a.dims.length;di++){
            FlowCore.DimensionStat ds=a.dims[di];
            ps.append("● ").append(ds.name).append(" · 최종 ")
                    .append(ds.finalVote==0?"중립":FlowCore.sideLabel(ds.name,ds.finalVote))
                    .append("  (+표 ").append(ds.plusVotes).append(" / -표 ").append(ds.minusVotes)
                    .append(" / 기권 ").append(ds.abstain).append(")\n");
            for(FlowCore.PatternStat p:ds.patterns){
                ps.append("  최근").append(p.length).append(": ").append(p.label(ds.name)).append("\n");
            }
            if(di<a.dims.length-1)ps.append("\n");
        }
        patterns.setText(ps.toString());

        FlowCore.Backtest b=a.backtest;
        backtest.setText(
                "API30 내부 순차재현: "+stat(b.hit,b.n)+
                "\n\n[3차원 최종방향 적중]"+
                "\n좌/우: "+stat(b.dimHit[0],b.dimN[0])+
                "\n사다리수: "+stat(b.dimHit[1],b.dimN[1])+
                "\n홀/짝: "+stat(b.dimHit[2],b.dimN[2])+
                "\n\n[등급별 삼치기]"+
                "\n⚠️ 약: "+stat(b.gradeHit[0],b.gradeN[0])+
                "\n🟡 보통: "+stat(b.gradeHit[1],b.gradeN[1])+
                "\n🔥 강: "+stat(b.gradeHit[2],b.gradeN[2])+
                "\n\n※ 이 재현값은 현재 API30 안의 참고값 · 실전 누적과 별도"+
                "\n기준 삼치기: 75.0%"
        );

        android.content.SharedPreferences sp=FlowCore.prefs(this);
        int n=sp.getInt(FlowCore.K_LIVE_TOTAL,0),hit=sp.getInt(FlowCore.K_LIVE_SUCCESS,0);
        double lp=Double.longBitsToDouble(sp.getLong(FlowCore.K_LIVE_PROFIT,Double.doubleToLongBits(0)));
        live.setText("V7 실전 · "+(n>0?hit+"/"+n+" = "+FlowCore.pct((double)hit/n)+" · 누적 "+FlowCore.signed(lp):"아직 없음"));

        int st=Math.max(5000,sp.getInt(FlowCore.K_BASE_STAKE,5000));
        double o=Math.max(1.01,sp.getFloat(FlowCore.K_ODDS,1.95f));
        double bt=b.hit*FlowCore.successProfit(st,o)-(b.n-b.hit)*3.0*st;
        profit.setText(
                "1회 총 배팅: "+FlowCore.money(3.0*st)+" ("+FlowCore.money(st)+" × 3개)"+
                "\n삼치기 성공(2/3): "+FlowCore.signed(FlowCore.successProfit(st,o))+
                "\n제외조합 출현(0/3): "+FlowCore.signed(-3.0*st)+
                "\n손익분기 성공률: "+FlowCore.pct(FlowCore.breakEven(o))+
                "\nAPI30 내부 순차재현 가상손익: "+FlowCore.signed(bt)
        );

        List<FlowCore.Result> td=FlowCore.recentDesc(d,15);
        StringBuilder rr=new StringBuilder();
        for(int i=0;i<Math.min(15,td.size());i++){
            FlowCore.Result r=td.get(i);
            rr.append(i==0?"최신  ":"      ").append(r.round).append("회 · ").append(FlowCore.COMBO[r.combo]);
            if(i<Math.min(15,td.size())-1)rr.append("\n");
        }
        recent.setText(rr.length()==0?"결과 없음":rr.toString());
    }

    private String stat(int h,int n){
        return n==0?"-":h+"/"+n+" = "+FlowCore.pct((double)h/n);
    }

    private String gradeIcon(String g){
        return "강".equals(g)?"🔥":"보통".equals(g)?"🟡":"⚠️";
    }

    private void startAutoService(){
        Intent i=new Intent(this,AutoDrawService.class);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
    }

    private void stopAutoService(){
        stopService(new Intent(this,AutoDrawService.class));
    }

    private void updateBgState(){
        boolean on=FlowCore.prefs(this).getBoolean(FlowCore.K_AUTO,true);
        if(bgState!=null){
            bgState.setText(on?"● 백그라운드 자동추첨 ON":"○ 백그라운드 OFF");
            bgState.setTextColor(on?Color.rgb(52,211,153):Color.GRAY);
        }
    }

    private void registerUpdates(){
        IntentFilter f=new IntentFilter(FlowCore.ACTION_UPDATED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver,f);
    }

    private void requestNotificationPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTI);
        }
    }

    private void startExport(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"BubbleBinary30V7_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.KOREA).format(new Date())+".json");
        startActivityForResult(i,REQ_EXPORT);
    }

    private void startImport(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i,REQ_IMPORT);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();
        try{
            if(req==REQ_EXPORT){
                OutputStream o=getContentResolver().openOutputStream(u);
                if(o==null)throw new Exception("파일 열기 실패");
                o.write(FlowCore.backup(this).toString(2).getBytes("UTF-8"));
                o.close();
                Toast.makeText(this,"V7 백업 완료",Toast.LENGTH_LONG).show();
            }else if(req==REQ_IMPORT){
                InputStream in=getContentResolver().openInputStream(u);
                if(in==null)throw new Exception("파일 열기 실패");
                BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));
                StringBuilder sb=new StringBuilder();
                String line;
                while((line=br.readLine())!=null)sb.append(line);
                br.close();
                FlowCore.restore(this,new JSONObject(sb.toString()));
                loadSettings();
                reloadAsync();
                Toast.makeText(this,"가져오기 완료 · 다음 API 조회부터 최신30회로 다시 교체",Toast.LENGTH_LONG).show();
            }
        }catch(Exception e){
            Toast.makeText(this,"처리 실패: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void confirmReset(){
        new AlertDialog.Builder(this)
                .setTitle("V7 실전성적 초기화")
                .setMessage("실전 승패·수익만 초기화합니다. 현재 API30 분석 스냅샷은 그대로 보존됩니다.")
                .setNegativeButton("취소",null)
                .setPositiveButton("초기화",(d,w)->{
                    FlowCore.resetPerformance(this);
                    Toast.makeText(this,"실전 성적만 초기화 완료",Toast.LENGTH_SHORT).show();
                    reloadAsync();
                }).show();
    }

    private LinearLayout card(){
        LinearLayout x=new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setPadding(dp(14),dp(14),dp(14),dp(14));
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(15,30,46));
        g.setCornerRadius(dp(18));
        x.setBackground(g);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,dp(12));
        x.setLayoutParams(lp);
        return x;
    }

    private TextView section(String s){
        TextView v=tv(s,16,Color.WHITE,true);
        v.setPadding(0,0,0,dp(10));
        return v;
    }

    private TextView tv(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }

    private Button button(String s,int color){
        Button b=new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
        return b;
    }

    private EditText input(String s){
        EditText e=new EditText(this);
        e.setText(s);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.GRAY);
        e.setTextSize(16);
        e.setPadding(dp(12),0,dp(12),0);
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(30,41,59));
        g.setCornerRadius(dp(12));
        e.setBackground(g);
        return e;
    }

    private int dp(int v){
        return (int)(v*getResources().getDisplayMetrics().density+.5f);
    }

    @Override protected void onDestroy(){
        h.removeCallbacksAndMessages(null);
        try{unregisterReceiver(receiver);}catch(Exception ignored){}
        ex.shutdownNow();
        super.onDestroy();
    }
}
