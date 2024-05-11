package ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.inputmethod.EditorInfo
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
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class DeltaPluginActivity : AppCompatActivity() {

    private lateinit var shellOutputTextView: TextView
        private lateinit var deltaPluginButton: Button
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
                        copyButton = findViewById(R.id.copy_button) // Reference the new button

                        commandInput.setOnEditorActionListener { _, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                executeCommand()
                                return@setOnEditorActionListener true
                            }
                            false
                        }

                        deltaPluginButton.setOnClickListener {
                            executeCommand()
                        }

                        copyButton.setOnClickListener {
                            copyTextToClipboard()
                        }
                    }

                    private fun executeCommand() {
                        val command = findViewById<EditText>(R.id.command_input).text.toString()
                        val workingDir = "/data/data/$packageName/files/resources/"

                        val output = shellExec(command, workingDir)
                        shellOutputTextView.text = output // Set the entire text view content
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
                        val gamePath = PreferenceManager.getDefaultSharedPreferences(ctx)
                        .getString("game_files", "")!!
                        prefs = PreferenceManager.getDefaultSharedPreferences(this)


                        val newFilePath = Constants.USER_CONFIG + "/delta.cfg" // Create a new path for delta.cfg
                        val addonFilePath = gamePath + "/'Data Files'" + "/delta-merged.omwaddon"

                        // Copy openmw.cfg to delta.cfg
                        val sourcePath = Paths.get("Constants.USER_CONFIG + \"/openmw.cfg\"")
                        val destinationPath = Paths.get("Constants.USER_CONFIG + \"/delta.cfg\"")

                        try {
                            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
                            println("File copied successfully.")
                        } catch (e: Exception) {
                            println("An error occurred: ${e.message}")
                        }

                        val deltaoutput = "content=\"$gamePath/Data Files\""
                        File(newFilePath).appendText("\n" + deltaoutput) // Append data to the copied delta.cfg
                    

                        try {
                            val processBuilder = ProcessBuilder()
                            if (workingDir != null) {
                                processBuilder.directory(File(workingDir))
                            }
                            System.setProperty("HOME", "/data/data/$packageName/files/")
                            // Build the command with relative path (use the copied delta.cfg)
                            val commandToExecute = arrayOf("/system/bin/sh", "-c", "export HOME=/data/data/$packageName/files/; chmod u+x ./delta_plugin; ./delta_plugin -c $newFilePath merge $cmd $addonFilePath")
                            processBuilder.command(*commandToExecute)
                            processBuilder.redirectErrorStream(true) // Merge stderr into stdout
                            val process = processBuilder.start()

                            val deltamergeoutput = "data=\"$gamePath/Data Files/delta-merged.omwaddon\""
                            File(Constants.USER_CONFIG + "/openmw.cfg").appendText("\n" + deltamergeoutput) // Append data to the copied delta.cfg


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
