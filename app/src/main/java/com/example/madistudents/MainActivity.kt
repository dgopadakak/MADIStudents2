package com.example.madistudents

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.madistudents.databinding.ActivityMainBinding
import com.example.madistudents.forRecyclerView.CustomRecyclerAdapterForExams
import com.example.madistudents.forRecyclerView.RecyclerItemClickListener
import com.example.madistudents.ui.faculty.DbHelper
import com.example.madistudents.ui.faculty.Group
import com.example.madistudents.ui.faculty.GroupOperator
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener
{
    private val gsonBuilder = GsonBuilder()
    private val gson: Gson = gsonBuilder.create()
    private val serverIP = "192.168.1.69"
    private val serverPort = 9876
    private lateinit var connection: Connection
    private var connectionStage: Int = 0
    private lateinit var dbh: DbHelper
    private var dbVersion = 2

    private lateinit var nv: NavigationView
    private var startTime: Long = 0
    //private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewExams: RecyclerView

    private val go: GroupOperator = GroupOperator()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener{ view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        nv = binding.navView
        nv.setNavigationItemSelectedListener(this)
        ///toolbar = findViewById(R.id.toolbar)
        //toolbar.apply { setNavigationIcon(android.R.drawable.ic_menu_sort_by_size) }
        progressBar = findViewById(R.id.progressBar)
        recyclerViewExams = findViewById(R.id.recyclerViewExams)
        recyclerViewExams.visibility = View.INVISIBLE
        recyclerViewExams.layoutManager = LinearLayoutManager(this)

        recyclerViewExams.addOnItemTouchListener(
            RecyclerItemClickListener(
                recyclerViewExams,
                object : RecyclerItemClickListener.OnItemClickListener
                {
                    override fun onItemClick(view: View, position: Int)
                    {

                    }
                    override fun onItemLongClick(view: View, position: Int)
                    {

                    }
                }
            )
        )

        dbh = DbHelper(this, "MyFirstDB", null, dbVersion)
        startTime = System.currentTimeMillis()
        connection = Connection(serverIP, serverPort, "{R}", this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    internal inner class Connection(
        private val SERVER_IP: String,
        private val SERVER_PORT: Int,
        private val refreshCommand: String,
        private val activity: Activity
    ) {
        private var outputServer: PrintWriter? = null
        private var inputServer: BufferedReader? = null
        var thread1: Thread? = null
        private var threadT: Thread? = null

        internal inner class Thread1Server : Runnable {
            override fun run()
            {
                val socket: Socket
                try {
                    socket = Socket(SERVER_IP, SERVER_PORT)
                    outputServer = PrintWriter(socket.getOutputStream())
                    inputServer = BufferedReader(InputStreamReader(socket.getInputStream()))
                    Thread(Thread2Server()).start()
                    sendDataToServer(refreshCommand)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        internal inner class Thread2Server : Runnable {
            override fun run() {
                while (true) {
                    try {
                        val message = inputServer!!.readLine()
                        if (message != null)
                        {
                            activity.runOnUiThread { processingInputStream(message) }
                        } else {
                            thread1 = Thread(Thread1Server())
                            thread1!!.start()
                            return
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        internal inner class Thread3Server(private val message: String) : Runnable
        {
            override fun run()
            {
                outputServer!!.write(message)
                outputServer!!.flush()
            }
        }

        internal inner class ThreadT : Runnable
        {
            override fun run() {
                while (true)
                {
                    if (System.currentTimeMillis() - startTime > 5000L && connectionStage == 0)
                    {
                        activity.runOnUiThread { val toast = Toast.makeText(
                            applicationContext,
                            "Подключиться не удалось!\n" +
                                    "Будут использоваться данные из локальной базы данных.",
                            Toast.LENGTH_LONG
                        )
                            toast.show() }
                        connectionStage = -1
                        progressBar.visibility = View.INVISIBLE
                        val tempArrayListGroups: ArrayList<Group> = dbh.getAllData()
                        go.setGroups(tempArrayListGroups)
                        for (i in 0 until tempArrayListGroups.size)
                        {
                            activity.runOnUiThread { nv.menu.add(0, i, 0,
                                tempArrayListGroups[i].name as CharSequence) }
                        }
                    }
                }
            }
        }

        fun sendDataToServer(text: String)
        {
            Thread(Thread3Server(text + "\n")).start()
        }

        private fun processingInputStream(text: String)
        {
            dbh.removeAllData()
            val tempGo: GroupOperator = gson.fromJson(text, GroupOperator::class.java)
            for (i in tempGo.getGroups())
            {
                dbh.insertGroup(i)
            }
            if (connectionStage == 0)
            {
                val toast = Toast.makeText(
                    applicationContext,
                    "Успешно подключено!\n" +
                            "Будут использоваться данные, полученные от сервера.",
                    Toast.LENGTH_LONG
                )
                toast.show()
            }
            connectionStage = 1
            progressBar.visibility = View.INVISIBLE
            val tempArrayListGroups: ArrayList<Group> = dbh.getAllData()
            go.setGroups(tempArrayListGroups)
            for (i in 0 until tempArrayListGroups.size)
            {
                nv.menu.add(0, i, 0,
                    tempArrayListGroups[i].name as CharSequence)
            }
        }

        init {
            thread1 = Thread(Thread1Server())
            thread1!!.start()
            threadT = Thread(ThreadT())
            threadT!!.start()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val toast = Toast.makeText(
            applicationContext,
            "Выбрана группа: $item.",
            Toast.LENGTH_SHORT
        )
        toast.show()
        recyclerViewExams.adapter = CustomRecyclerAdapterForExams(go.getExamsNames(item.itemId),
            go.getTeachersNames(item.itemId))
        recyclerViewExams.visibility = View.VISIBLE
        return true
    }
}