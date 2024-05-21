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
        val commandInput = findViewById<EditText>(R.id.command_input)

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

        var lines = File(Constants.USER_CONFIG + "/delta.cfg").readLines().toMutableList()
        lines.removeAll { it.contains("content=delta-merged.omwaddon") }
        lines.removeAll { it.contains("content=builtin.omwscripts") }
        lines.removeAll { it.contains("data=\"$gamePath/Data Files\"") }

        // Write the modified lines back to the file
        File(Constants.USER_CONFIG + "/delta.cfg").writeText(lines.joinToString("\n"))

        val newFilePath = Constants.USER_CONFIG + "/delta.cfg" // Create a new path for delta.cfg
        val deltaoutput = "data=\"$gamePath/Data Files\""
        File(newFilePath).appendText("\n" + deltaoutput) // Append data to the copied delta.cfg

        val command = "NUM_RAYON_THREADS=1 ./delta_plugin -c " + Constants.USER_CONFIG + "/delta.cfg merge --skip-cells " + findViewById<EditText>(R.id.command_input).text.toString() + " " + gamePath + "/'Data Files'/delta-merged.omwaddon"

        var lines2 = File(Constants.USER_CONFIG + "/openmw.cfg").readLines().toMutableList()
        lines2.removeAll { it.contains("content=delta-merged.omwaddon") }

        // Write the modified lines back to the file
        File(Constants.USER_CONFIG + "/openmw.cfg").writeText(lines2.joinToString("\n"))

        val deltamergeoutput = "content=delta-merged.omwaddon"
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltamergeoutput) // Append data to the copied delta.cfg

        val workingDir = "/data/data/$packageName/files/resources/"
        val output = shellExec(command, workingDir)
        shellOutputTextView.append(output + "\n\n")
    }

    // New function for the second button
    private fun executeSpecialCommand() {
        val gamePath = PreferenceManager.getDefaultSharedPreferences(ctx)
                .getString("game_files", "")!!
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val specialWorkingDir = "/data/data/$packageName/files/resources/"

        var lines = File(Constants.USER_CONFIG + "/openmw.cfg").readLines().toMutableList()
        lines.removeAll { it.contains("groundcover=output_groundcover.omwaddon") }
        lines.removeAll { it.contains("content=output_deleted.omwaddon") }

        // Write the modified lines back to the file
        File(Constants.USER_CONFIG + "/openmw.cfg").writeText(lines.joinToString("\n"))

        val newFilePath = Constants.USER_CONFIG + "/delta.cfg" // Create a new path for delta.cfg
        val deltaoutput = "data=\"$gamePath/Data Files\""
        File(newFilePath).appendText("\n" + deltaoutput) // Append data to the copied delta.cfg

        // Define the specific commands to execute for this button here
        var Command = "NUM_RAYON_THREADS=1 ./delta_plugin -c " + Constants.USER_CONFIG + "/delta.cfg filter --all --output $gamePath/'Data Files'/output_groundcover.omwaddon --desc \"Generated groundcover plugin from your local cavebros\" match Static --id \"grass|kelp|lilypad\" --modify model \"^\" \"grass\\\\\" match Cell --cellref-object-id \"grass|kelp|lilypad\"" + " && " + "NUM_RAYON_THREADS=1 ./delta_plugin -c " + Constants.USER_CONFIG + "/delta.cfg filter --all --output $gamePath/'Data Files'/output_deleted.omwaddon match Cell --cellref-object-id \"grass|kelp|lilypad\" --delete" + " && " + "NUM_RAYON_THREADS=1 ./delta_plugin -c \" + Constants.USER_CONFIG + \"/delta.cfg query --input $gamePath/'Data Files'/output_groundcover.omwaddon --ignore $gamePath/'Data Files'/deleted_groundcover.omwaddon match Static"


        val deltagrounddeleteoutput = "content=output_deleted.omwaddon"
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltagrounddeleteoutput)
        val deltagroundoutput = "groundcover=output_groundcover.omwaddon"
        File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltagroundoutput)


        var output = shellExec(Command, specialWorkingDir)
        val outputlines = output.split("\n") // Split the output into lines
        val modelLines = outputlines.filter { it.trim().startsWith("model:") }
        val paths = modelLines.map { it.substringAfter("model: \"grass").replace("\\\\", "/").trim().replace("\"", "") }
        shellOutputTextView.append((paths + "\n\n").toString())

        paths.forEach { path ->

            val filename = path.substringAfterLast("/")
            val correctedPath = path.substringBeforeLast("/").trim()

            Command = "mkdir -p $gamePath/'Data Files'/Meshes/grass/$correctedPath" + " && " + "NUM_RAYON_THREADS=1 ./delta_plugin -c " + Constants.USER_CONFIG + "/delta.cfg vfs-extract \"Meshes$correctedPath/$filename\" $gamePath/'Data Files'/Meshes/grass/$correctedPath/$filename"

            output = shellExec(Command, specialWorkingDir)
            shellOutputTextView.append(output)
        }

        output = shellExec(Command, specialWorkingDir)
        shellOutputTextView.append(output + "\n\n")
    }

    // New function for the second button
    private fun executeQueryCommand() {
        // Define the specific commands to execute for this button here
        val Command = "NUM_RAYON_THREADS=1 ./delta_plugin -c " + Constants.USER_CONFIG + "/delta.cfg query " + findViewById<EditText>(R.id.command_input).text.toString()
        val specialWorkingDir = "/data/data/$packageName/files/resources/" // Optional, use same if not needed
        val output = shellExec(Command, specialWorkingDir)
        shellOutputTextView.append(output + "\n\n")
    }

    private fun updateTextView() {
        val output = shellExec()
        // Append the new output to the existing text
        shellOutputTextView.append(output)
        // Schedule another update after a short delay (e.g., 1 second)
        handler.postDelayed(updateTextRunnable, 1000)
    }

    private fun shellExec(cmd: String? = null, workingDir: String? = null): String {
        var output = ""
        var inputStreamReader: BufferedReader? = null
        var errorStreamReader: BufferedReader? = null

        try {
            val processBuilder = ProcessBuilder()
            if (workingDir != null) {
                processBuilder.directory(File(workingDir))
            }
            System.setProperty("HOME", "/data/data/$packageName/files/")
            // Build the command with relative path (use the copied delta.cfg)
            val commandToExecute = arrayOf("/system/bin/sh", "-c", "export HOME=/data/data/$packageName/files/; chmod u+x ./delta_plugin; $cmd")
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
