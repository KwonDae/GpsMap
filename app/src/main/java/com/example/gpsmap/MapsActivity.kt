package com.example.gpsmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    //위치정보를 주기적으로 얻는데 필요한 객체들을 선언
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: MyLocationCallBack
    private val REQUEST_ACCESS_FINE_LOCATION = 1000
    // PolyLine 옵션
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED)

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면이 꺼지지 않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 세모모드로 화면 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_maps)

        //SupportMapFragment를 가져와서 지도가 준비되면 알림을 받습니다.
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) // 지도가 준비되면 알람을 받습니다.

        locationInit() //(1)에서 선언한 변수들을 초기화하는 메소드 호출


    }

    //위치 정보를 얻기 위한 각종 초기화

    // LocationRequest는 위치 정보 요청에 대한 세부 정보를 설정.
    // 이 앱은 가장 정확한 위치를 요구하면서 10초마다 위치 정보를 갱신.
    private fun locationInit() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        locationCallback = MyLocationCallBack()

        locationRequest = LocationRequest()
        // GPS우선
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        /*
        업데이트 인터벌
                위치 정보가 없을 때는 업데이트 안함
                상황에 따라 짧아질 수 있음, 정확하지 않음
        다른 앱에서 짧은 인터벌로 위치 정보를 요청하면 짧아질 수 있음
        */

        locationRequest.interval = 10000
        //정확함. 이것보다 짧은 업데이트는 하지 않음
        locationRequest.fastestInterval = 5000
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

    /*
    사용 가능한 맵을 조작합니다.
    지도를 사용할 준비가 되면 이 콜백이 호출됩니다.
    여기서 마커나 선, 청취자를 추가하거나 카메라를 이동할 수 있습니다.
    호주 시드니 근처에 마커를 추가하고 있습니다.
    gOOGLE play 서비스가 기기에 설치되어 있지 않은 경우 사용자에게
    supportMapfragment 안에 google play 서비스를 설치하고 앱으로
    돌아온 후에만 호출됩니다.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap // 지도가 준비되면 GoogleMap 객체를 얻습니다

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onResume() {
        super.onResume()
        addLocationListener()

        //권한 요청
        permissionCheck(cancel = {
            //위치 정보가 필요한 이유 다이얼로그 표시
            showPermissionInfoDialog()

        }, ok = {
            // 현재 위치를 주기적으로 요청(권한이 필요한 부분)
            addLocationListener()
        })
    }


    private fun addLocationListener() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)


    }

    // requestLocationUpdates() 메서드에 전달되는 인자 중 LocationCallBack을 구현한 내부 클래스는
    // LocationResult 객체를 반환하고 lastLocation 객체를 반환하고 lastLocation 프로퍼티로 Location 객체를 얻는다.
    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            val location = locationResult?.lastLocation
            // 기기의 gps설정이 꺼져 있거나 현재 위치 정보를 불러 올 수 없는 경우
            location?.run {
                val latLng = LatLng(latitude, longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))

                Log.d("MapsActivity", "위도: $latitude, 경도: $longitude")

                // PolyLine에 좌표추가
                polylineOptions.add(latLng)

                //선 그리기
                mMap.addPolyline(polylineOptions)
            }
        }
    }

    private fun permissionCheck(cancel: () -> Unit, ok: () -> Unit) {
        //위치 권한이 있는지 검사
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //권한이 허용되지 않음
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                //이전에 권한을 한 번 거부한 적이 있는 경우에 실행할 함수
                cancel()
            } else {
                //권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION
                )
            }
        } else {
            //권한을 수락했을 떄 실행할 함수
            ok()
        }
    }

    private fun showPermissionInfoDialog() {
        alert("현재 위치 정보를 얻으려면 위치 권한이 필요합니다", "권한이 필요한 이유") {
            yesButton {
                ActivityCompat.requestPermissions(
                    this@MapsActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION
                )
            }
            noButton {}
        }.show()
    }

    override fun onPause() {
        super.onPause()
        // 위치 요청 취소
        removeLocationListener()
    }

    private fun removeLocationListener() {
        //현재 위치 요청을 언제 삭제
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 권한 허용 됨
                    addLocationListener()
                }
                else {
                    // 권한 거부
                    toast("권한 거부 됨")
                }
                return
            }
        }
    }

}