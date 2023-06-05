package com.itson.blutut

import android.content.ClipData.Item
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.gson.Gson
import com.ingenieriajhr.blujhr.BluJhr
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    //bluetooth var
    lateinit var blue: BluJhr
    var devicesBluetooth = ArrayList<String>()

    //visible ListView
    var graphviewVisible = true

    //graphviewSeries
    lateinit var temperatura: LineGraphSeries<DataPoint?>

    //nos indica si estamos recibiendo datos o no
    var initGraph = false
    //nos almacena el estado actual de la conexion bluetooth
    var stateConn = BluJhr.Connected.False

    //valor que se suma al eje x despues de cada actualizacion
    var ejeX = 0.6

    //sweet alert necesarios
    lateinit var loadSweet : SweetAlertDialog
    lateinit var errorSweet : SweetAlertDialog
    lateinit var okSweet : SweetAlertDialog
    lateinit var disconnection : SweetAlertDialog

    lateinit var menu: Menu;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        initSweet()

        blue = BluJhr(this)
        blue.onBluetooth()

        val btnViewDevice = findViewById<Button>(R.id.btnViewDevice)
        btnViewDevice.setOnClickListener {
            when (graphviewVisible) {
                false -> invisibleListDevice()
                true -> visibleListDevice()
            }
        }

        val listDeviceBluetooth = findViewById<ListView>(R.id.listDeviceBluetooth)
        listDeviceBluetooth.setOnItemClickListener { adapterView, view, i, l ->
            if (devicesBluetooth.isNotEmpty()) {
                initSweet()
                blue.closeConnection()
                blue.connect(devicesBluetooth[i])
                //genera error si no se vuelve a iniciar los objetos sweet
                initSweet()
                blue.setDataLoadFinishedListener(object : BluJhr.ConnectedBluetooth {
                    override fun onConnectState(state: BluJhr.Connected) {
                        stateConn = state
                        when (state) {
                            BluJhr.Connected.True -> {
                                loadSweet.dismiss()
                                okSweet.show()
                                blue.bluTx("E")
                                invisibleListDevice()
                                rxReceived()
                            }
                            BluJhr.Connected.Pending -> {
                                loadSweet.show()
                            }
                            BluJhr.Connected.False -> {
                                loadSweet.dismiss()
                                errorSweet.show()
                            }
                            BluJhr.Connected.Disconnect -> {
                                loadSweet.dismiss()
                                disconnection.show()
                                visibleListDevice()
                            }
                        }
                    }
                })
            }
        }
        //graphview
        initGraph()
        val btnInitStop = findViewById<Button>(R.id.btnInitStop)
        btnInitStop.setOnClickListener {
            if (stateConn == BluJhr.Connected.True){
                initGraph = when(initGraph){
                    true->{
                        blue.bluTx("0")
                        btnInitStop.text = "START"
                        false
                    }
                    false->{
                        blue.bluTx("1")
                        btnInitStop.text = "STOP"
                        true
                    }
                }
            }
        }

        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.Switch -> {blue.bluTx("F") }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun rxReceived() {
        val gson = Gson()
        val txtTemp = findViewById<TextView>(R.id.txtTemp)
        blue.loadDateRx(object:BluJhr.ReceivedData{
            override fun rxDate(rx: String) {
                println("------------------- RX $rx --------------------")
                ejeX+=0.6
                blue.bluTx("E")
                val objeto = gson.fromJson(rx,objeto::class.java)
                temperatura.appendData(DataPoint(objeto.valor_x.toDouble(), objeto.valor_y.toDouble()),true, 200)
                val i =1

            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!blue.stateBluetoooth() && requestCode == 100){
            blue.initializeBluetooth()
        }else{
            if (requestCode == 100){
                devicesBluetooth = blue.deviceBluetooth()
                if (devicesBluetooth.isNotEmpty()){
                    val listDeviceBluetooth = findViewById<ListView>(R.id.listDeviceBluetooth)
                    val adapter = ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,devicesBluetooth)
                    listDeviceBluetooth.adapter = adapter
                }else{
                    Toast.makeText(this, "No tienes vinculados dispositivos", Toast.LENGTH_SHORT).show()
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (blue.checkPermissions(requestCode,grantResults)){
            Toast.makeText(this, "Exit", Toast.LENGTH_SHORT).show()
            blue.initializeBluetooth()
        }else{
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
                blue.initializeBluetooth()
            }else{
                Toast.makeText(this, "Algo salio mal", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

     private fun initSweet() {
        loadSweet = SweetAlertDialog(this,SweetAlertDialog.PROGRESS_TYPE)
        okSweet = SweetAlertDialog(this,SweetAlertDialog.SUCCESS_TYPE)
        errorSweet = SweetAlertDialog(this,SweetAlertDialog.ERROR_TYPE)
        disconnection = SweetAlertDialog(this,SweetAlertDialog.NORMAL_TYPE)

        loadSweet.titleText = "Conectando"
        loadSweet.setCancelable(false)
        errorSweet.titleText = "Algo salio mal"

        okSweet.titleText = "Conectado"
        disconnection.titleText = "Desconectado"
    }

    private fun initGraph() {
        val graph = findViewById<GraphView>(R.id.graph)
        //permitime controlar los ejes manualmente
        graph.viewport.isXAxisBoundsManual = true;
        graph.viewport.setMinX(0.0);
        graph.viewport.setMaxX(10.0);
        graph.viewport.setMaxY(60.0)
        graph.viewport.setMinY(0.0)

        //permite realizar zoom y ajustar posicion eje x
        graph.viewport.isScalable = true
        graph.viewport.setScalableY(true)
        graph.viewport.isScrollable = true

        temperatura = LineGraphSeries()
        //draw points
        temperatura.isDrawDataPoints = true;
        //draw below points
        temperatura.isDrawBackground = true;
        //color series
        temperatura.color = Color.RED

        //opcionales
        //temperatura.setTitle("temp")
        //graph.getLegendRender().setVisible(true)
        //graph.getLegendRender().setAlign(LegenderRender.LegendAlign.TOP)
        graph.addSeries(temperatura);
    }

    private fun invisibleListDevice() {
        initSweet()
        val containerGraph = findViewById<LinearLayout>(R.id.containerGraph)
        val containerDevice = findViewById<LinearLayout>(R.id.containerDevice)
        val btnViewDevice = findViewById<Button>(R.id.btnViewDevice)
        containerGraph.visibility = View.VISIBLE
        containerDevice.visibility = View.GONE
        graphviewVisible = true
        btnViewDevice.text = "DEVICE"
    }

    private fun visibleListDevice() {
        initSweet()
        val containerGraph = findViewById<LinearLayout>(R.id.containerGraph)
        val containerDevice = findViewById<LinearLayout>(R.id.containerDevice)
        val btnViewDevice = findViewById<Button>(R.id.btnViewDevice)
        containerGraph.visibility = View.GONE
        containerDevice.visibility = View.VISIBLE
        graphviewVisible = false
        btnViewDevice.text = "GraphView"

    }


}