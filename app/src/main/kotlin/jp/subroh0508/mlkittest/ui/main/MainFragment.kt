package jp.subroh0508.mlkittest.ui.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import jp.subroh0508.mlkittest.R
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        = inflater.inflate(R.layout.main_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        barcodeScanning?.setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.toBarcodeScanningFragment)
        )
        faceDetection?.setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.toFaceDetectionFragment)
        )
        imageLabeling?.setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.toImageLabelingFragment)
        )
        textRecognition?.setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.toTextRecognitionFragment)
        )
    }
}
