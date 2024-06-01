package ui.activity

import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.libopenmw.openmw.R
import constants.Constants
import org.jetbrains.anko.ctx
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class DeltaPluginActivity : AppCompatActivity() {

    private lateinit var shellOutputTextView: TextView
    private lateinit var deltaPluginButton: Button
    private lateinit var deltaGrassButton: Button
    private lateinit var deltaQueryButton: Button
    private lateinit var copyButton: Button // New Button for copying
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var prefs: SharedPreferences? = null
    private val updateTextRunnable = Runnable { updateTextView() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delta_plugin_view)

        val dir = File(Constants.USER_FILE_STORAGE + "/delta")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        shellOutputTextView = findViewById(R.id.myTextView)
        deltaPluginButton = findViewById(R.id.delta_plugin_button)
        deltaGrassButton = findViewById(R.id.delta_grass_button)
        deltaQueryButton = findViewById(R.id.delta_query_button)
        copyButton = findViewById(R.id.copy_button) // Reference the new button

        deltaPluginButton.setOnClickListener {
            executeCommand()
        }

        deltaGrassButton.setOnClickListener {
            executeSpecialCommand()
        }

        deltaQueryButton.setOnClickListener {
            executeQueryCommand()
        }

        copyButton.setOnClickListener {
            copyTextToClipboard()
        }

        val textView = findViewById<TextView>(R.id.memory_info)

        runnable = Runnable {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)

            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            val usedMemory = totalMemory - availableMemory

            textView.text = "Total memory: ${humanReadableByteCountBin(totalMemory)}\n" +
                    "Available memory: ${humanReadableByteCountBin(availableMemory)}\n" +
                    "Used memory: ${humanReadableByteCountBin(usedMemory)}"

            handler.postDelayed(runnable, 1000)
        }

        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun humanReadableByteCountBin(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp-1] + "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    private fun executeCommand() {
        val gamePath = PreferenceManager.getDefaultSharedPreferences(ctx)
                .getString("game_files", "")!!
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the Application Context
        val context = getApplicationContext()

        // Get the nativeLibraryDir (might not be suitable for this case)
        val applicationInfo = context.applicationInfo
        val WorkingDir = applicationInfo.nativeLibraryDir

        val deltaConfigFile = File(Constants.USER_CONFIG + "/delta.cfg")
        if (!deltaConfigFile.exists()) {
            Toast.makeText(this, "delta.cfg not detected, Please launch the game for it to be created", Toast.LENGTH_SHORT).show()
            return
        }

        var lines = File(Constants.USER_CONFIG + "/delta.cfg").readLines().toMutableList()
        lines.removeAll { it.contains("content=delta-merged.omwaddon") }
        lines.removeAll { it.contains("data=" + Constants.USER_DELTA) }
        lines.removeAll { it.contains("data=\"$gamePath/Data Files\"") }

        // Write the modified lines back to the file
        File(Constants.USER_CONFIG + "/delta.cfg").writeText(lines.joinToString("\n"))

        var lines2 = File(Constants.USER_CONFIG + "/openmw.cfg").readLines().toMutableList()
        lines2.removeAll { it.contains("content=delta-merged.omwaddon") }
        lines2.removeAll { it.contains("data=" + Constants.USER_DELTA) }

        // Write the modified lines back to the file
        File(Constants.USER_CONFIG + "/openmw.cfg").writeText(lines2.joinToString("\n"))

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Running Delta Plugin...") // Set the message
        progressDialog.setCancelable(false) // Set cancelable to false
        progressDialog.show() // Show the ProgressDialog

        val newFilePath = Constants.USER_CONFIG + "/delta.cfg" // Create a new path for delta.cfg
        val deltaoutput = "data=\"$gamePath/Data Files\""
        File(newFilePath).appendText("\n" + deltaoutput) // Append data to the copied delta.cfg

        val Command = "./libdelta_plugin.so -v --verbose -c " + Constants.USER_CONFIG + "/delta.cfg merge --skip-cells " + findViewById<EditText>(R.id.command_input).text.toString() + " " + Constants.USER_DELTA + "/delta-merged.omwaddon"

        val deltamergeoutput = "content=delta-merged.omwaddon"
        val deltapath = "data=" + Constants.USER_DELTA
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltapath + "\n" + deltamergeoutput) // Append data to the copied delta.cfg

        // Execute the command in a separate thread
        Thread {
            val output = shellExec(Command, WorkingDir)
            runOnUiThread {
                shellOutputTextView.append(output)
                progressDialog.dismiss()
            }

            // Write output to a file
            try {
                val file = File(Constants.USER_CONFIG + "/delta.log")
                file.printWriter().use { out ->
                    out.println(output)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }.start()
        //handler.postDelayed(updateTextRunnable, 1000)
    }

    private fun executeSpecialCommand(pretend: Boolean = false) {
        val gamePath = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("game_files", "")!!

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val context = getApplicationContext()
        val applicationInfo = context.applicationInfo
        val WorkingDir = applicationInfo.nativeLibraryDir

        val deltaConfigFile = File(Constants.USER_CONFIG + "/delta.cfg")
        if (!deltaConfigFile.exists()) {
            Toast.makeText(this, "delta.cfg not detected, Please launch the game for it to be created", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize the ProgressDialog
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Running Groundcoverify...") // Set the message
        progressDialog.setCancelable(false) // Set cancelable to false
        progressDialog.show() // Show the ProgressDialog

        // Execute the command in a separate thread
        Thread {
            val gamePath = PreferenceManager.getDefaultSharedPreferences(ctx)
                .getString("game_files", "")!!
            prefs = PreferenceManager.getDefaultSharedPreferences(this)

            var lines2 = File(Constants.USER_CONFIG + "/delta.cfg").readLines().toMutableList()
            lines2.removeAll { it.contains("data=\"$gamePath/Data Files\"") }

            // Write the modified lines back to the file
            File(Constants.USER_CONFIG + "/delta.cfg").writeText(lines2.joinToString("\n"))

            val lines = File(Constants.USER_CONFIG + "/openmw.cfg").readLines().toMutableList()
            lines.removeAll { it.contains("groundcover=output_groundcover.omwaddon") || it.contains("content=output_deleted.omwaddon") }

            val deltagrounddeleteoutput = "content=output_deleted.omwaddon"
            val deltagroundoutput = "groundcover=output_groundcover.omwaddon"
            lines.add(deltagrounddeleteoutput)
            lines.add(deltagroundoutput)

            File(Constants.USER_CONFIG + "/openmw.cfg").writeText(lines.joinToString("\n"))

            val newFilePath = Constants.USER_CONFIG + "/delta.cfg" // Create a new path for delta.cfg
            val deltaoutput = "data=\"$gamePath/Data Files\""
            File(newFilePath).appendText("\n" + deltaoutput) // Append data to the copied delta.cfg

            val grassIds = "grass|kelp|lilypad|fern|thirrlily|spartium|in_cave_plant|reedgroup"
            val excludeIds = "refernce|infernace|planter|_furn_|_skelp|t_glb_var_skeleton|cliffgrass|terr|grassplane|flora_s_m_10_grass|cave_mud_rocks_fern|ab_in_cavemold|rp_mh_rock|ex_cave_grass00|secret_fern"
            val ids_expr = "^(?!.*($excludeIds).*).*($grassIds).*$"
            val exteriorCellRegex = "^[0-9\\-]+x[0-9\\-]+$"

            val Command = StringBuilder("./libdelta_plugin.so -v --verbose -c ")
            Command.append(Constants.USER_CONFIG + "/delta.cfg filter --all --output ")
            Command.append(Constants.USER_DELTA + "/output_groundcover.omwaddon --desc \"Generated groundcover plugin from your local cavebros\" match Cell --cellref-object-id \"$ids_expr\" --id \"$exteriorCellRegex\" match Static --id \"$ids_expr\" --modify model \"^\" \"grass\\\\\"")
            if (!pretend) {
                Command.append(" && ")
                Command.append("./libdelta_plugin.so -v --verbose -c " + Constants.USER_CONFIG + "/delta.cfg filter --all --output " + Constants.USER_DELTA + "/output_deleted.omwaddon match Cell --cellref-object-id \"$ids_expr\" --id \"$exteriorCellRegex\" --delete")
            }
            Command.append(" && ")
            Command.append("./libdelta_plugin.so -v --verbose -c " + Constants.USER_CONFIG + "/delta.cfg query --input " + Constants.USER_DELTA + "/output_groundcover.omwaddon --ignore " + Constants.USER_DELTA + "/deleted_groundcover.omwaddon match Static")

            var output = shellExec(Command.toString(), WorkingDir)
            val outputlines = output.split("\n")
            val modelLines = outputlines.filter { it.trim().startsWith("model:") }
            val paths = modelLines.map { it.substringAfter("model: \"grass").replace("\\\\", "/").trim().replace("\"", "") }

            runOnUiThread {
                shellOutputTextView.append(output)
                try {
                    val file = File(Constants.USER_CONFIG + "/groundcoverify.log")
                    file.printWriter().use { out ->
                        out.println(output)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                shellOutputTextView.append((paths + "\n").toString())
            }

            paths.forEach { path ->
                val filename = path.substringAfterLast("/")
                val correctedPath = path.substringBeforeLast("/").trim()

                val Command2 = StringBuilder("mkdir -p ")
                Command2.append(Constants.USER_DELTA + "/Meshes/grass/$correctedPath")
                Command2.append(" && ")
                Command2.append("./libdelta_plugin.so -v --verbose -c ")
                Command2.append(Constants.USER_CONFIG + "/delta.cfg vfs-extract \"Meshes$correctedPath/$filename\" ")
                Command2.append(Constants.USER_DELTA + "/Meshes/grass/$correctedPath/$filename")

                output = shellExec(Command2.toString(), WorkingDir)
                runOnUiThread {
                    shellOutputTextView.append(output)
                    progressDialog.dismiss() // Dismiss the ProgressDialog
                }
            }

        }.start()
        //handler.postDelayed(updateTextRunnable, 1000)
    }
    
    private fun executeQueryCommand() {
        // Get the Application Context
        val context = applicationContext

        // Get the nativeLibraryDir (might not be suitable for this case)
        val WorkingDir = context.applicationInfo.nativeLibraryDir

        val deltaConfigFile = File(Constants.USER_CONFIG + "/delta.cfg")
        if (!deltaConfigFile.exists()) {
            Toast.makeText(this, "delta.cfg not detected, Please launch the game for it to be created", Toast.LENGTH_SHORT).show()
            return
        }

        // Define the specific commands to execute for this button here
        val commandInput = findViewById<EditText>(R.id.command_input).text.toString()
        val Command = "./libdelta_plugin.so -v --verbose -c ${Constants.USER_CONFIG}/delta.cfg query $commandInput"

        val output = shellExec(Command, WorkingDir)
        shellOutputTextView.append("$output")
    }

    private fun updateTextView() {
        // Execute the shell command in a separate thread
        Thread {
            try {
                val output = shellExec()
                // Update the TextView on the main thread
                runOnUiThread {
                    // Append the new output to the existing text
                    shellOutputTextView.append(output)
                }
            } catch (e: Exception) {
                // Handle any exceptions
                e.printStackTrace()
            }
        }.start()

        // Schedule another update after a short delay (e.g., 1 second)
        handler.postDelayed(updateTextRunnable, 1000)
    }

    private fun shellExec(cmd: String? = null, WorkingDir: String? = null): String {
        val output = StringBuilder()
        try {
            val processBuilder = ProcessBuilder()
            if (WorkingDir != null) {
                processBuilder.directory(File(WorkingDir))
            }
            System.setProperty("HOME", "/data/data/$packageName/files/")
            val commandToExecute = arrayOf("/system/bin/sh", "-c", "export HOME=/data/data/$packageName/files/; $cmd")
            processBuilder.command(*commandToExecute)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            process.inputStream.bufferedReader().use { inputStreamReader ->
                var line: String?
                while (inputStreamReader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }

            process.waitFor()
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            output.append("Error executing command: ").append(e.message).append("\nStacktrace:\n").append(sw.toString())
        }

        findViewById<EditText>(R.id.command_input).text.clear()
        return output.toString()
    }

    private fun copyTextToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Shell Output", shellOutputTextView.text.toString())
        clipboard.setPrimaryClip(clipData)

        // Optional: Show a toast message to indicate successful copy
        Toast.makeText(this, "Shell Output Copied", Toast.LENGTH_SHORT).show()
    }
}
