package errorreporter.submission

@JvmRecord
data class Submission(
    val metadata: SubmissionMetadata,
    val errors: List<SubmissionError>,
)

@JvmRecord
data class SubmissionMetadata(
    val pluginName: String,
    val pluginVersion: String,
    val osName: String,
    val javaVersion: String,
    val javaVmVendor: String,
    val isEap: Boolean,
    val appName: String,
    val ideaBuild: String,
    val ideaVersion: String,
    val lastAction: String?,
)

@JvmRecord
data class SubmissionError(
    val message: String,
    val description: String?,
    val stacktrace: String,
    val attachments: List<SubmissionAttachment>,
)

@JvmRecord
data class SubmissionAttachment(val name: String, val displayText: String, val body: String?, val bytes: String?)
