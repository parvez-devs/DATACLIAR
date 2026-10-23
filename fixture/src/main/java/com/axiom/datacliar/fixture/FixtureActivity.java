package com.axiom.datacliar.fixture;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Only installed in disposable CI emulators. Never included in release assets. */
public final class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        try{write(new File(getFilesDir(),"keep.txt"));write(new File(getCacheDir(),"temporary.txt"));}catch(Exception e){throw new IllegalStateException(e);}
        TextView text=new TextView(this);text.setText("Disposable DATACLIAR control fixture");setContentView(text);
    }
    private void write(File file)throws Exception{try(FileOutputStream out=new FileOutputStream(file)){out.write("DATACLIAR_FIXTURE".getBytes(StandardCharsets.UTF_8));}}
}
