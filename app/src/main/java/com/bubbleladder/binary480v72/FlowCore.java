package com.bubbleladder.binary480v72;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public final class FlowCore {
    private FlowCore(){}

    public static final String API="https://api.bepick.io/game/bubble_ladder3";
    public static final String PREF="bubble_binary480_v72";
    public static final String ACTION_UPDATED="com.bubbleladder.binary480v72.FLOW_UPDATED";

    public static final String K_HISTORY="rolling480_history_v72", K_RECORDS="records_v72",
            K_PENDING_IDX="pending_idx_v72", K_PENDING_EXCLUDE="pending_exclude_v72",
            K_PENDING_STAKE="pending_stake_v72", K_PENDING_ODDS="pending_odds_v722",
            K_PENDING_GRADE="pending_grade_v72",
            K_LIVE_TOTAL="live_total_v72", K_LIVE_SUCCESS="live_success_v72",
            K_LIVE_PROFIT="live_profit_v72", K_BASE_STAKE="base_stake_v72",
            K_ODDS="odds_v72", K_AUTO="auto_enabled_v72",
            K_LAST_EXCLUDE="last_exclude_v72", K_LAST_TRIPLE="last_triple_v72",
            K_LAST_GRADE="last_grade_v72", K_LAST_MODE="last_mode_v72",
            K_LAST_SYNC="last_sync_v72", K_LAST_API_COUNT="last_api_count_v72";

    public static final int WINDOW=480;
    public static final String[] COMBO={"","좌3짝","좌4홀","우3홀","우4짝"};
    public static final String[] DIM={"좌/우","사다리수","홀/짝"};

    // +1 = 좌 / 3줄 / 홀, -1 = 우 / 4줄 / 짝
    private static final int[][] VEC={
            {0,0,0},
            {+1,+1,-1},
            {+1,-1,+1},
            {-1,+1,+1},
            {-1,-1,-1}
    };

    public static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public static final class Result{
        public long idx;
        public String date;
        public int round,combo;
    }

    public static final class PatternStat{
        public int length,matches,plusNext,minusNext,vote;
        public String label(String dim){
            if(matches==0)return "같은 흐름 없음";
            if(vote==0)return matches+"회 · +"+plusNext+" / -"+minusNext+" · 동률 기권";
            return matches+"회 · +"+plusNext+" / -"+minusNext+" · "+sideLabel(dim,vote)+" 1표";
        }
    }

    public static final class DimensionStat{
        public String name;
        public PatternStat[] patterns=new PatternStat[4];
        public int plusVotes,minusVotes,abstain,totalMatches,finalVote;
    }

    private static final class FlowDecision{
        int exclude,topOppose,secondOppose,decisiveVotes;
        int[] oppose=new int[5];
        DimensionStat[] dims=new DimensionStat[3];
        String mode,grade;
    }

    public static final class Backtest{
        public int n,hit;
        public int[] dimN=new int[3],dimHit=new int[3];
        public int[] gradeN=new int[3],gradeHit=new int[3];
    }

    public static final class Analysis{
        public int exclude,count480,topOppose,secondOppose,decisiveVotes;
        public int[] oppose=new int[5];
        public String date,triple,selected,grade,mode,suffix,windowRange;
        public DimensionStat[] dims;
        public Backtest backtest;
    }

    public static final class SyncResult{
        public boolean newRoundResolved;
        public Analysis analysis;
        public List<Result> history;
    }

    public static List<Result> fetch() throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.setUseCaches(false);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("User-Agent","BubbleBinary480/7.2");
        int code=c.getResponseCode();
        if(code<200||code>=300)throw new Exception("API HTTP "+code);
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
        StringBuilder sb=new StringBuilder();
        String line;
        while((line=br.readLine())!=null)sb.append(line);
        br.close();
        c.disconnect();

        JSONObject root=new JSONObject(sb.toString());
        JSONArray arr=root.optJSONArray("data");
        if(arr==null)throw new Exception("API data 없음");
        List<Result> out=new ArrayList<>();
        for(int i=0;i<arr.length();i++){
            JSONObject o=arr.optJSONObject(i);
            if(o==null)continue;
            int combo=o.optInt("fd4",0);
            long idx=o.optLong("idx",0);
            if(idx<=0||combo<1||combo>4)continue;
            Result r=new Result();
            r.idx=idx;
            r.date=o.optString("date","");
            r.round=o.optInt("round",0);
            r.combo=combo;
            out.add(r);
        }
        out.sort((a,b)->Long.compare(b.idx,a.idx));
        if(out.isEmpty())throw new Exception("결과 없음");
        return out;
    }

    public static List<Result> load(Context c){
        List<Result> out=new ArrayList<>();
        String raw=prefs(c).getString(K_HISTORY,"");
        if(raw==null||raw.isEmpty())return out;
        try{
            JSONArray a=new JSONArray(raw);
            for(int i=0;i<a.length();i++){
                JSONObject j=a.optJSONObject(i);
                if(j==null)continue;
                Result r=new Result();
                r.idx=j.optLong("i");
                r.date=j.optString("d","");
                r.round=j.optInt("r",0);
                r.combo=j.optInt("c",0);
                if(r.idx>0&&r.combo>=1&&r.combo<=4)out.add(r);
            }
        }catch(Exception ignored){}
        out.sort((a,b)->Long.compare(b.idx,a.idx));
        if(out.size()>WINDOW)out=new ArrayList<>(out.subList(0,WINDOW));
        return out;
    }

    public static void save(Context c,List<Result> list){
        try{
            JSONArray a=new JSONArray();
            int n=Math.min(WINDOW,list.size());
            for(int i=0;i<n;i++){
                Result r=list.get(i);
                JSONObject o=new JSONObject();
                o.put("i",r.idx);o.put("d",r.date);o.put("r",r.round);o.put("c",r.combo);
                a.put(o);
            }
            prefs(c).edit().putString(K_HISTORY,a.toString()).putInt(K_LAST_API_COUNT,n).apply();
        }catch(Exception ignored){}
    }

    // 핵심: API는 매번 최신 스냅샷을 받고, idx 중복을 제거해 최근 최대 480회만 누적 유지한다.
    public static SyncResult sync(Context c)throws Exception{
        List<Result> before=load(c);
        long latestBefore=before.isEmpty()?-1:before.get(0).idx;
        List<Result> api=fetch();

        TreeMap<Long,Result> map=new TreeMap<>(Collections.reverseOrder());
        for(Result r:before)map.put(r.idx,r);
        for(Result r:api)map.put(r.idx,r);

        List<Result> merged=new ArrayList<>(map.values());
        if(merged.size()>WINDOW)merged=new ArrayList<>(merged.subList(0,WINDOW));

        boolean resolved=resolvePending(c,merged);
        save(c,merged);
        Analysis a=analyze(merged);
        savePending(c,merged,a);
        prefs(c).edit().putLong(K_LAST_SYNC,System.currentTimeMillis())
                .putInt(K_LAST_API_COUNT,api.size()).apply();

        SyncResult sr=new SyncResult();
        sr.newRoundResolved=resolved||(!merged.isEmpty()&&merged.get(0).idx!=latestBefore);
        sr.analysis=a;sr.history=merged;
        return sr;
    }

    public static Analysis analyze(List<Result> desc){
        if(desc==null||desc.isEmpty())return null;
        List<Result> all=chronoAsc(desc);
        if(all.size()>WINDOW)all=new ArrayList<>(all.subList(all.size()-WINDOW,all.size()));
        int end=all.size();
        FlowDecision d=flowDecision(all,0,end);

        Analysis a=new Analysis();
        a.exclude=d.exclude;a.count480=end;a.date=dayKey(all.get(end-1).date);
        a.triple=tripleFor(d.exclude);a.selected=selectedThree(d.exclude);
        a.grade=d.grade;a.mode=d.mode;a.topOppose=d.topOppose;a.secondOppose=d.secondOppose;
        a.decisiveVotes=d.decisiveVotes;a.dims=d.dims;
        System.arraycopy(d.oppose,0,a.oppose,0,d.oppose.length);
        a.suffix=suffixText(all,end,6);a.windowRange=rangeLabel(all,0,end);
        a.backtest=backtest(all);
        return a;
    }

    private static FlowDecision flowDecision(List<Result> all,int start,int end){
        FlowDecision d=new FlowDecision();
        int[] lens={3,4,5,6};
        for(int dim=0;dim<3;dim++){
            DimensionStat ds=new DimensionStat();ds.name=DIM[dim];
            for(int i=0;i<lens.length;i++){
                PatternStat ps=patternStat(all,start,end,lens[i],dim);
                ds.patterns[i]=ps;ds.totalMatches+=ps.matches;
                if(ps.vote>0){ds.plusVotes++;d.decisiveVotes++;}
                else if(ps.vote<0){ds.minusVotes++;d.decisiveVotes++;}
                else ds.abstain++;
            }
            ds.finalVote=ds.plusVotes>ds.minusVotes?+1:ds.minusVotes>ds.plusVotes?-1:0;
            d.dims[dim]=ds;
        }

        for(int combo=1;combo<=4;combo++){
            int opp=0;
            for(int dim=0;dim<3;dim++)for(PatternStat ps:d.dims[dim].patterns){
                if(ps.vote!=0&&ps.vote!=VEC[combo][dim])opp++;
            }
            d.oppose[combo]=opp;
        }

        int max=-1;List<Integer> leaders=new ArrayList<>();
        for(int c=1;c<=4;c++){
            if(d.oppose[c]>max){max=d.oppose[c];leaders.clear();leaders.add(c);}
            else if(d.oppose[c]==max)leaders.add(c);
        }
        if(leaders.size()==1){
            d.exclude=leaders.get(0);d.mode="Rolling480 이진 최근3~6 · 반대표 최다";
        }else{
            int best=-1;List<Integer> second=new ArrayList<>();
            for(int c:leaders){
                int x=0;
                for(int dim=0;dim<3;dim++){
                    int fv=d.dims[dim].finalVote;
                    if(fv!=0&&fv!=VEC[c][dim])x++;
                }
                if(x>best){best=x;second.clear();second.add(c);}
                else if(x==best)second.add(c);
            }
            if(second.size()==1){d.exclude=second.get(0);d.mode="반대표 동률 → 3차원 다수결";}
            else{d.exclude=tieByAbsence(all,start,end,second);d.mode="동률 → Rolling480 미출현 간격";}
        }
        d.topOppose=d.oppose[d.exclude];d.secondOppose=0;
        for(int c=1;c<=4;c++)if(c!=d.exclude)d.secondOppose=Math.max(d.secondOppose,d.oppose[c]);
        int gap=d.topOppose-d.secondOppose;
        if(d.decisiveVotes>=9&&gap>=3)d.grade="강";
        else if(d.decisiveVotes>=6&&gap>=2)d.grade="보통";
        else d.grade="약";
        return d;
    }

    private static PatternStat patternStat(List<Result>a,int start,int end,int len,int dim){
        PatternStat ps=new PatternStat();ps.length=len;
        if(end-start<=len)return ps;
        for(int next=start+len;next<end;next++){
            boolean same=true;
            for(int j=0;j<len;j++){
                int cur=VEC[a.get(end-len+j).combo][dim];
                int old=VEC[a.get(next-len+j).combo][dim];
                if(cur!=old){same=false;break;}
            }
            if(same){
                ps.matches++;
                int n=VEC[a.get(next).combo][dim];
                if(n>0)ps.plusNext++;else ps.minusNext++;
            }
        }
        if(ps.plusNext>ps.minusNext)ps.vote=+1;
        else if(ps.minusNext>ps.plusNext)ps.vote=-1;
        return ps;
    }

    private static int tieByAbsence(List<Result>a,int start,int end,List<Integer> candidates){
        int best=candidates.get(0),bestGap=-1;
        for(int c:candidates){
            int gap=end-start+1;
            for(int i=end-1;i>=start;i--)if(a.get(i).combo==c){gap=end-1-i;break;}
            if(gap>bestGap){bestGap=gap;best=c;}
            else if(gap==bestGap&&c<best)best=c;
        }
        return best;
    }

    // 화면 참고용: 현재 보유한 최근 최대480회 안에서 각 목표회차 이전 데이터만 사용해 순차 재현한다.
    private static Backtest backtest(List<Result>all){
        Backtest b=new Backtest();
        for(int t=7;t<all.size();t++){
            int start=Math.max(0,t-WINDOW);
            FlowDecision d=flowDecision(all,start,t);
            int actual=all.get(t).combo;
            boolean ok=actual!=d.exclude;
            b.n++;if(ok)b.hit++;
            int gi=gradeIndex(d.grade);b.gradeN[gi]++;if(ok)b.gradeHit[gi]++;
            for(int dim=0;dim<3;dim++){
                int fv=d.dims[dim].finalVote;
                if(fv!=0){b.dimN[dim]++;if(fv==VEC[actual][dim])b.dimHit[dim]++;}
            }
        }
        return b;
    }

    private static List<Result> chronoAsc(List<Result>desc){
        List<Result>copy=new ArrayList<>(desc);copy.sort(Comparator.comparingLong(x->x.idx));return copy;
    }
    public static List<Result> recentDesc(List<Result>desc,int limit){
        List<Result>copy=new ArrayList<>(desc);copy.sort((a,b)->Long.compare(b.idx,a.idx));
        if(copy.size()>limit)return new ArrayList<>(copy.subList(0,limit));return copy;
    }
    private static String dayKey(String s){
        String digits=String.valueOf(s==null?"":s).replaceAll("\\D","");
        if(digits.length()>=8)return digits.substring(0,8);return String.valueOf(s==null?"":s);
    }
    private static String suffixText(List<Result>a,int end,int max){
        int from=Math.max(0,end-max);StringBuilder sb=new StringBuilder();
        for(int i=from;i<end;i++){if(sb.length()>0)sb.append(" → ");sb.append(COMBO[a.get(i).combo]);}
        return sb.toString();
    }
    private static String rangeLabel(List<Result>a,int start,int end){
        if(end<=start)return "-";Result first=a.get(start),last=a.get(end-1);
        return first.date+" "+first.round+"회 → "+last.date+" "+last.round+"회";
    }
    public static String sideLabel(String dim,int v){
        if("좌/우".equals(dim))return v>0?"좌":"우";
        if("사다리수".equals(dim))return v>0?"3줄":"4줄";
        return v>0?"홀":"짝";
    }
    private static int gradeIndex(String g){return "강".equals(g)?2:"보통".equals(g)?1:0;}

    private static void savePending(Context c,List<Result>d,Analysis a){
        if(d.isEmpty()||a==null)return;
        SharedPreferences sp=prefs(c);long next=nextIdx(d.get(0));long existing=sp.getLong(K_PENDING_IDX,-1);
        if(existing==next||existing>0)return;
        int stake=Math.max(5000,sp.getInt(K_BASE_STAKE,5000));double odds=Math.max(1.01,sp.getFloat(K_ODDS,1.95f));
        sp.edit().putLong(K_PENDING_IDX,next).putInt(K_PENDING_EXCLUDE,a.exclude)
                .putInt(K_PENDING_STAKE,stake).putFloat(K_PENDING_ODDS,(float)odds)
                .putString(K_PENDING_GRADE,a.grade).putInt(K_LAST_EXCLUDE,a.exclude)
                .putString(K_LAST_TRIPLE,a.triple).putString(K_LAST_GRADE,a.grade)
                .putString(K_LAST_MODE,a.mode).apply();
    }

    private static boolean resolvePending(Context c,List<Result>d){
        SharedPreferences sp=prefs(c);long idx=sp.getLong(K_PENDING_IDX,-1);int exc=sp.getInt(K_PENDING_EXCLUDE,0);
        if(idx<=0||exc<1||exc>4)return false;
        Result actual=null;for(Result r:d)if(r.idx==idx){actual=r;break;}
        if(actual==null)return false;
        boolean ok=actual.combo!=exc;int st=sp.getInt(K_PENDING_STAKE,5000);double o=sp.getFloat(K_PENDING_ODDS,1.95f);
        String grade=sp.getString(K_PENDING_GRADE,"약");double pnl=ok?successProfit(st,o):-3.0*st;
        int n=sp.getInt(K_LIVE_TOTAL,0)+1,hit=sp.getInt(K_LIVE_SUCCESS,0)+(ok?1:0);
        double old=Double.longBitsToDouble(sp.getLong(K_LIVE_PROFIT,Double.doubleToLongBits(0)));
        appendRecord(c,idx,exc,actual.combo,grade,ok,pnl);
        sp.edit().putInt(K_LIVE_TOTAL,n).putInt(K_LIVE_SUCCESS,hit)
                .putLong(K_LIVE_PROFIT,Double.doubleToLongBits(old+pnl))
                .remove(K_PENDING_IDX).remove(K_PENDING_EXCLUDE).remove(K_PENDING_STAKE)
                .remove(K_PENDING_ODDS).remove(K_PENDING_GRADE).apply();
        return true;
    }

    private static void appendRecord(Context c,long idx,int exc,int actual,String grade,boolean ok,double pnl){
        try{
            SharedPreferences sp=prefs(c);JSONArray a=new JSONArray(sp.getString(K_RECORDS,"[]"));
            JSONObject o=new JSONObject();o.put("idx",idx);o.put("exclude",exc);o.put("actual",actual);
            o.put("grade",grade);o.put("ok",ok);o.put("pnl",pnl);a.put(o);
            JSONArray out=new JSONArray();for(int i=Math.max(0,a.length()-1500);i<a.length();i++)out.put(a.get(i));
            sp.edit().putString(K_RECORDS,out.toString()).apply();
        }catch(Exception ignored){}
    }

    public static long nextIdx(Result r){
        try{
            String dk=dayKey(r.date);
            if(r.round<480)return Long.parseLong(dk.substring(2,8)+String.format(Locale.US,"%04d",r.round+1));
            SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.US);Calendar c=Calendar.getInstance();
            c.setTime(f.parse(dk));c.add(Calendar.DAY_OF_MONTH,1);String d=f.format(c.getTime());
            return Long.parseLong(d.substring(2,8)+"0001");
        }catch(Exception e){return r.idx+1;}
    }
    public static long millisToNextDraw(){long interval=180000L,now=System.currentTimeMillis();long mod=Math.floorMod(now,interval);long left=interval-mod;return left==0?interval:left;}
    public static String countdownText(){long s=(millisToNextDraw()+999)/1000;return String.format(Locale.KOREA,"%02d:%02d",s/60,s%60);}
    public static String tripleFor(int c){switch(c){case 1:return "우 + 4줄 + 홀";case 2:return "우 + 3줄 + 짝";case 3:return "좌 + 4줄 + 짝";case 4:return "좌 + 3줄 + 홀";default:return "-";}}
    public static String selectedThree(int exc){StringBuilder sb=new StringBuilder();for(int k=1;k<=4;k++){if(k==exc)continue;if(sb.length()>0)sb.append(" / ");sb.append(COMBO[k]);}return sb.toString();}
    public static double successProfit(int stake,double odds){return stake*(2*odds-3);}
    public static double breakEven(double odds){return 3/(2*odds);}
    public static String pct(double v){return String.format(Locale.KOREA,"%.1f%%",v*100);}
    public static String money(double v){return String.format(Locale.KOREA,"%,.0f원",v);}
    public static String signed(double v){return (v>=0?"+":"")+money(v);}
    public static String liveRate(Context c){SharedPreferences sp=prefs(c);int n=sp.getInt(K_LIVE_TOTAL,0),h=sp.getInt(K_LIVE_SUCCESS,0);return n==0?"-":h+"/"+n+" ("+pct((double)h/n)+")";}

    public static JSONObject backup(Context c)throws Exception{
        SharedPreferences sp=prefs(c);JSONObject root=new JSONObject();root.put("format","BubbleBinary480V72Backup");
        root.put("history",new JSONArray(sp.getString(K_HISTORY,"[]")));root.put("records",new JSONArray(sp.getString(K_RECORDS,"[]")));
        JSONObject st=new JSONObject();st.put(K_LIVE_TOTAL,sp.getInt(K_LIVE_TOTAL,0));st.put(K_LIVE_SUCCESS,sp.getInt(K_LIVE_SUCCESS,0));
        st.put(K_LIVE_PROFIT,sp.getLong(K_LIVE_PROFIT,Double.doubleToLongBits(0)));st.put(K_BASE_STAKE,sp.getInt(K_BASE_STAKE,5000));
        st.put(K_ODDS,sp.getFloat(K_ODDS,1.95f));st.put(K_AUTO,sp.getBoolean(K_AUTO,true));root.put("state",st);return root;
    }

    public static void restore(Context c,JSONObject root)throws Exception{
        SharedPreferences.Editor ed=prefs(c).edit();
        JSONObject st=root.optJSONObject("state");

        JSONArray src=null;
        if(root.has("history"))src=root.getJSONArray("history");
        else if(root.has("snapshot"))src=root.getJSONArray("snapshot");

        if(src!=null){
            TreeMap<Long,JSONObject> map=new TreeMap<>(Collections.reverseOrder());
            for(int i=0;i<src.length();i++){
                JSONObject o=src.optJSONObject(i);
                if(o==null)continue;
                long idx=o.optLong("i",o.optLong("idx",0));
                if(idx>0)map.put(idx,o);
            }
            JSONArray cut=new JSONArray();
            int n=0;
            for(JSONObject o:map.values()){
                if(n++>=WINDOW)break;
                cut.put(o);
            }
            ed.putString(K_HISTORY,cut.toString());
        }

        if(root.has("records"))ed.putString(K_RECORDS,root.getJSONArray("records").toString());
        if(st!=null){
            if(st.has(K_LIVE_TOTAL))ed.putInt(K_LIVE_TOTAL,st.optInt(K_LIVE_TOTAL,0));
            if(st.has(K_LIVE_SUCCESS))ed.putInt(K_LIVE_SUCCESS,st.optInt(K_LIVE_SUCCESS,0));
            if(st.has(K_LIVE_PROFIT))ed.putLong(K_LIVE_PROFIT,st.optLong(K_LIVE_PROFIT,Double.doubleToLongBits(0)));
            if(st.has(K_BASE_STAKE))ed.putInt(K_BASE_STAKE,Math.max(5000,st.optInt(K_BASE_STAKE,5000)));
            if(st.has(K_ODDS))ed.putFloat(K_ODDS,(float)st.optDouble(K_ODDS,1.95));
            if(st.has(K_AUTO))ed.putBoolean(K_AUTO,st.optBoolean(K_AUTO,true));
        }
        ed.apply();
    }

    // 성적만 초기화. Rolling480 분석 history와 설정은 보존한다.
    public static void resetPerformance(Context c){
        prefs(c).edit().remove(K_RECORDS).remove(K_PENDING_IDX).remove(K_PENDING_EXCLUDE)
                .remove(K_PENDING_STAKE).remove(K_PENDING_ODDS).remove(K_PENDING_GRADE)
                .remove(K_LIVE_TOTAL).remove(K_LIVE_SUCCESS).remove(K_LIVE_PROFIT).apply();
    }
}
