package app.cap.beshop;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;


public class popupActivity extends AppCompatActivity {
    final Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.custom_popup);
        TextView textView = (TextView)dialog.findViewById(R.id.tv_title);
        TextView textView1 = (TextView)dialog.findViewById(R.id.tv_content);
        Button button = (Button)dialog.findViewById(R.id.btn_ok);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.show();
    }
}
