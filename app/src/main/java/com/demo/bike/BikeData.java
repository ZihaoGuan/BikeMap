package com.demo.bike;
import com.google.android.gms.maps.model.LatLng;

//bike信息
//author: Jeff Zihao Guan
public class BikeData {
    private int id,battery;
    private String shebei_biaohao,product_id;
    private double latitude;
    private double longitude;
    private LatLng latLng;

    public BikeData(int id, String pid, double latitude, double longitude, String biaohao, int battery){
        this.id = id;
        this.product_id = pid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.shebei_biaohao = biaohao;
        this.battery = battery;
        setLatLng( this.latitude, this.longitude);
    }

    public int getBattery(){
        return this.battery;
    }

    public String getBiaohao(){
        return this.shebei_biaohao;
    }

    public String getPid(){
        return this.product_id;
    }

    public double getLat(){
        return this.latitude;
    }

    public double getLon(){
        return this.longitude;
    }

    public int getId(){
        return this.id;
    }

    public LatLng getLatLng(){
        return this.latLng;
    }

    private void setLatLng(double lat, double lon){
        this.latLng = new LatLng(-lat, lon);
    }

}
