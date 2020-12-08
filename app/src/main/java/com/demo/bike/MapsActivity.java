package com.demo.bike;

import androidx.fragment.app.FragmentActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//author: Jeff Zihao Guan
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    //用来存在Map上的标记
    public Map<String, Marker> markers = new HashMap<String, Marker>();
    //用来存从数据库查询到的data    thread safe List 防止在a线程处理list时 b线程遍历list出错
    public List<BikeData> bikeList = Collections.synchronizedList(new ArrayList<BikeData>());
    //从数据库查询数据的间隔 默认2秒
    public int fetchDataInterval = 2000;
    //重画marker的间隔 默认2秒
    public int refreshMarkerInterval = 2000;
    //数据库参数
    //public String database = "jdbc:mysql://10.0.2.2:3306/v";
    public String database = "jdbc:mysql://xo1.x10hosting.com:3306/manageon_manage?allowMultiQueries=true";
    //public String userName = "root";
    public String userName = "manageon";
    //public String password = "";
    public String password = "onzo2019";
    boolean firstTime = true;
    Button b;
    EditText t1,t2,t3;

    //测试模式toggle，展示时将不会连接数据库
    //测试时 为true
    boolean testMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (testMode){
            loadTestData();
        }else{
            //持续查询数据，展示模式时关闭
            new FetchData().start();
        }
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //方便在运行中更改服务器
        b = (Button)findViewById(R.id.button);
        t1 = (EditText)findViewById(R.id.editText1);
        t2 = (EditText)findViewById(R.id.editText2);
        t3 = (EditText)findViewById(R.id.editText3);
        if (!testMode){
            b.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    database = t1.getText().toString();
                    userName = t2.getText().toString();
                    password = t3.getText().toString();
                    Log.i("setDBParam",database+" "+userName+" "+password);
                }
            });
        }
    }

    //展示用数据
    private void loadTestData(){
        bikeList.add(new BikeData(1,"8000", 41.31,174.8,"biao1",1));
        bikeList.add(new BikeData(2,"8200", 41.32,174.9,"biao2",2));
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //持续刷新markers
        new UpdateMakersThread().start();
    }

    class FetchData extends Thread{
        public void run(){
            //隔一段时间查询数据
            while (true){
                try {
                    Thread.sleep(fetchDataInterval);
                }
                catch (InterruptedException ie)
                {
                    Log.e("InterruptedException_",ie.getMessage());
                }
                //清空上一次的数据
                bikeList.clear();
                //查询新一批数据
                doQuery();
            }
        }
    }

    class  UpdateMakersThread extends Thread{
        public void run(){
            UpdateMakersTask task = null;
            while (true){
                if(task == null || task.getStatus() == AsyncTask.Status.FINISHED){
                    task = new UpdateMakersTask();
                    task.execute();
                }
            }
        }
    }

    //等待一段时间 再画 不阻塞用户操作
    class UpdateMakersTask extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void[] a) {
            try {
                Thread.sleep(refreshMarkerInterval);
            }
            catch (InterruptedException ie)
            {
                Log.e("InterruptedException_",ie.getMessage());
            }
            return null;
        }
        protected void onPostExecute(Void result) {
            Log.i("onpost","!!!!!!!!!!!!!!");
            updateMarkers();
        }
    }

    //重画markers 这一段有参考网上的实现方案
    private void updateMarkers(){
        int id;
        BikeData bd;
        //临时存储用
        Map<String, Marker> updatedMarkers = new HashMap<String, Marker>();
        synchronized (bikeList){
            Iterator<BikeData> iterator = bikeList.iterator();
            while(iterator.hasNext()){
                bd = iterator.next();
                //如果 marker 存在的话就更新gps定位与电池状态， 如果是新的marker 才new 一个marker
                // if marker exists move its location, if not add new marker
                id = bd.getId();
                Marker marker = markers.get(id+"");
                if (marker == null)
                {
                    Log.i("new_marker","hey");
                    marker = mMap.addMarker(new MarkerOptions().position(bd.getLatLng())
                            .title("biaohao" + bd.getBiaohao() + "  " + bd.getBattery() + "%"));
                }
                else
                {
                    marker.setPosition(bd.getLatLng());
                    marker.setTitle("biaohao" + bd.getBiaohao() + "  " + bd.getBattery() + "%");
                    markers.remove(bd.getId()+"");
                }
                updatedMarkers.put(id+"", marker);
            }
        }
        //旧markers里剩下的marker没有与新数据中的任意一项相对应 应该被抹去
        for (Marker marker : markers.values())
        {
            marker.remove();
        }
        //保存新处理好的marker
        markers = updatedMarkers;

        //第一次画marker时 定位镜头用
        if (firstTime){
            if (markers.size() > 0){
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers.values())
                {
                    builder.include(marker.getPosition());
                }
                LatLngBounds bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,10);
                mMap.moveCamera(cu);
                firstTime = false;
            }
        }
    }

    //连接数据库 以及查询数据库
    private void doQuery(){
        Connection connection = null;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(database,userName,password);
            Log.i("fu", "远程连接成功!");
            Statement stm = connection.createStatement();
            //sql 查询车的定位 标号 电量等
            String sql= "SELECT b.id,b.product_id, b.battery,b.latitude, b.longitude, t.shebei_biaohao" +
                    "  FROM t_yw_shebei_number t" +
                    "  left outer join bike_lock b" +
                    "  on b.product_id = t.shebei_id";
            ResultSet rs = stm.executeQuery(sql);

            while(rs.next()){
                Log.i("result", "");
                bikeList.add(new BikeData(rs.getInt(1),rs.getString(2),
                        rs.getDouble(4), rs.getDouble(5), rs.getString(6),
                        rs.getInt(3)));
            }
            rs.close();
            stm.close();
        }
        catch(Exception e){
            for (StackTraceElement se:e.getStackTrace()){
                Log.i("sql",se.toString());
            }
        }
        finally {
            try{
                connection.close();
            }catch(Exception e){
                Log.i("sql",e.getMessage());
            }
        }
    }
}
