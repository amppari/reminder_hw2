package x.pitkanen.mobcomp.reminder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // TODO For testing only
        buttonTestBack.setOnClickListener {
            finish()
        }
    }
}
