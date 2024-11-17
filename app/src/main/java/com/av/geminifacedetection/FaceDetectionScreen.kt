import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.av.geminifacedetection.FaceDetectionViewModel
import com.av.geminifacedetection.R
import com.av.geminifacedetection.UiState

@Composable
fun FaceDetectionScreen(
    faceDetectionViewModel: FaceDetectionViewModel = viewModel()
) {
    val placeholderResult = stringResource(R.string.results_placeholder)
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by faceDetectionViewModel.uiState.collectAsState()
    val context = LocalContext.current


    // State to hold the selected image URI
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val placeholderImageRes =
        R.drawable.place_holder_image // replace with your placeholder image resource

    // Gallery launcher for selecting an image from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
        }
    }

    // Main UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = "",
                modifier = Modifier.padding(2.dp)
            )
            Text(
                text = stringResource(R.string.face_detection_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = selectedImageUri ?: placeholderImageRes,
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(400.dp)
                    .padding(16.dp)
            )

            // Button to pick an image from the gallery
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }
                    galleryLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Upload Image")
            }

            // Button to trigger face detection
            Button(
                onClick = {
                    selectedImageUri?.let { uri ->
                        // Convert URI to Bitmap here and pass it to the ViewModel
                        val bitmap = context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                        if (bitmap != null) {
                            faceDetectionViewModel.sendPrompt(bitmap)
                        }
                    } ?: run {
                        // If the selected image URI is null, show a toast
                        Toast.makeText(context, "Select an image first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.action_check_for_acceptance))
            }

            // Display loading or result
            if (uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                var textColor = MaterialTheme.colorScheme.onSurface
                result = when (uiState) {
                    is UiState.Error -> {
                        textColor = MaterialTheme.colorScheme.error
                        (uiState as UiState.Error).errorMessage
                    }

                    is UiState.Success -> (uiState as UiState.Success).outputText
                    else -> placeholderResult
                }

                // Scrollable text result
                val scrollState = rememberScrollState()
                Text(
                    text = result,
                    textAlign = TextAlign.Start,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                )
            }
        }
    }
}
