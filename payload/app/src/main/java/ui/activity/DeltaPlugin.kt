package ui.activity

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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter

class DeltaPluginActivity : AppCompatActivity() {

    private lateinit var shellOutputTextView: TextView
    private lateinit var deltaPluginButton: Button
    private lateinit var deltaGrassButton: Button
    private lateinit var deltaQueryButton: Button
    private lateinit var copyButton: Button // New Button for copying
    private val handler = Handler(Looper.getMainLooper())
    private var prefs: SharedPreferences? = null
    private val updateTextRunnable = Runnable { updateTextView() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delta_plugin_view)

        File(Constants.USER_FILE_STORAGE + "/delta").mkdirs()

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

        val newFilePath = Constants.USER_CONFIG + "/delta.cfg" // Create a new path for delta.cfg
        val deltaoutput = "data=\"$gamePath/Data Files\""
        File(newFilePath).appendText("\n" + deltaoutput) // Append data to the copied delta.cfg

        val command = "./libdelta_plugin.so -c " + Constants.USER_CONFIG + "/delta.cfg merge --skip-cells " + findViewById<EditText>(R.id.command_input).text.toString() + " " + Constants.USER_DELTA + "/delta-merged.omwaddon"

        val deltamergeoutput = "content=delta-merged.omwaddon"
        val deltapath = "data=" + Constants.USER_DELTA
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltamergeoutput + "\n" + deltapath) // Append data to the copied delta.cfg

        val output = shellExec(command, WorkingDir)
        shellOutputTextView.append(output + "\n\n")
    }

    // New function for the second button
    private fun executeSpecialCommand() {

        // Get the Application Context
        val context = getApplicationContext()

        // Get the nativeLibraryDir (might not be suitable for this case)
        val applicationInfo = context.applicationInfo
        val WorkingDir = applicationInfo.nativeLibraryDir

        var lines = File(Constants.USER_CONFIG + "/openmw.cfg").readLines().toMutableList()
        lines.removeAll { it.contains("groundcover=output_groundcover.omwaddon") }
        lines.removeAll { it.contains("content=output_deleted.omwaddon") }

        // Write the modified lines back to the file
        File(Constants.USER_CONFIG + "/openmw.cfg").writeText(lines.joinToString("\n"))

        // Define the specific commands to execute for this button here
        var Command = "./libdelta_plugin.so -c " + Constants.USER_CONFIG + "/delta.cfg filter --all --output " + Constants.USER_DELTA + "/output_groundcover.omwaddon --desc \"Generated groundcover plugin from your local cavebros\" match Static --id \"grass|kelp|lilypad\" --modify model \"^\" \"grass\\\\\" match Cell --cellref-object-id \"grass|kelp|lilypad\"" + " && " + "./libdelta_plugin.so -c " + Constants.USER_CONFIG + "/delta.cfg filter --all --output " + Constants.USER_DELTA + "/output_deleted.omwaddon match Cell --cellref-object-id \"grass|kelp|lilypad\" --delete" + " && " + "./libdelta_plugin.so -c " + Constants.USER_CONFIG + "/delta.cfg query --input " + Constants.USER_DELTA + "/output_groundcover.omwaddon --ignore " + Constants.USER_DELTA + "/deleted_groundcover.omwaddon match Static"

        val deltagrounddeleteoutput = "content=output_deleted.omwaddon"
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltagrounddeleteoutput)
        val deltagroundoutput = "groundcover=output_groundcover.omwaddon"
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltagroundoutput)

        var output = shellExec(Command, WorkingDir)
        val outputlines = output.split("\n") // Split the output into lines
        val modelLines = outputlines.filter { it.trim().startsWith("model:") }
        val paths = modelLines.map { it.substringAfter("model: \"grass").replace("\\\\", "/").trim().replace("\"", "") }
        shellOutputTextView.append((paths + "\n\n").toString())

        paths.forEach { path ->

            val filename = path.substringAfterLast("/")
            val correctedPath = path.substringBeforeLast("/").trim()

            Command = "mkdir -p " + Constants.USER_DELTA + "/Meshes/grass/$correctedPath" + " && " + "./libdelta_plugin.so -c " + Constants.USER_CONFIG + "/delta.cfg vfs-extract \"Meshes$correctedPath/$filename\" " + Constants.USER_DELTA + "/Meshes/grass/$correctedPath/$filename"

            output = shellExec(Command, WorkingDir)
            shellOutputTextView.append(output + "\n\n")
        }
    }

    private fun executeQueryCommand() {
        // Define the specific commands to execute for this button here
        val Command = "./libdelta_plugin.so -c " + Constants.USER_CONFIG + "/delta.cfg query " + findViewById<EditText>(R.id.command_input).text.toString()

        // Get the Application Context
        val context = getApplicationContext()

        // Get the nativeLibraryDir (might not be suitable for this case)
        val applicationInfo = context.applicationInfo
        val WorkingDir = applicationInfo.nativeLibraryDir

        val output = shellExec(Command, WorkingDir)
        shellOutputTextView.append(output + "\n\n")
    }


    private fun updateTextView() {
        val output = shellExec()
        // Append the new output to the existing text
        shellOutputTextView.append(output)
        // Schedule another update after a short delay (e.g., 1 second)
        handler.postDelayed(updateTextRunnable, 1000)
    }

    private fun shellExec(cmd: String? = null, WorkingDir: String? = null): String {
        var output = ""
        var inputStreamReader: BufferedReader? = null
        var errorStreamReader: BufferedReader? = null

        try {
            val processBuilder = ProcessBuilder()
            if (WorkingDir != null) {
                processBuilder.directory(File(WorkingDir))
            }
            System.setProperty("HOME", "/data/data/$packageName/files/")
            // Build the command with relative path (use the copied delta.cfg)
            val commandToExecute = arrayOf("/system/bin/sh", "-c", "export HOME=/data/data/$packageName/files/; $cmd")
            processBuilder.command(*commandToExecute)
            processBuilder.redirectErrorStream(true) // Merge stderr into stdout
            val process = processBuilder.start()

            inputStreamReader = BufferedReader(InputStreamReader(process.inputStream))
            errorStreamReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (inputStreamReader.readLine().also { line = it } != null) {
                output += line + "\n" // Add newline for better formatting
            }

            // Check for errors in the error stream
            while (errorStreamReader.readLine().also { line = it } != null) {
                output += "Rust Error: $line\n" // Prefix Rust errors for clarity
            }

            process.waitFor() // Wait for the process to finish
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            output = "Error executing command: ${e.message}\nStacktrace:\n${sw.toString()}"
        } finally {
            inputStreamReader?.close() // Close the reader even if there's an exception
            errorStreamReader?.close() // Close the reader even if there's an exception
        }
        // Clear the command after execution (optional)
        findViewById<EditText>(R.id.command_input).text.clear()
        return output
    }

    private fun copyTextToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Shell Output", shellOutputTextView.text.toString())
        clipboard.setPrimaryClip(clipData)

        // Optional: Show a toast message to indicate successful copy
        Toast.makeText(this, "Shell Output Copied", Toast.LENGTH_SHORT).show()
    }
}
