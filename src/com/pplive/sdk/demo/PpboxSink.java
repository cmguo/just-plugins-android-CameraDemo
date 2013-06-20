package com.pplive.sdk.demo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import android.view.SurfaceHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import com.pplive.sdk.PPBOX;
import com.pplive.thirdparty.BreakpadUtil;

public class PpboxSink {

	private long capture;
	
	private Camera camera;
	
	private AudioRecord audio;
	
	private PpboxStream video_stream;
	
	private PpboxStream audio_stream;
	
	private Thread audio_thread;
	
	//private long free_index = 0;
	//private Vector<PPBOX.Sample> samples;

	public static void init(Context c)
	{
		File cacheDirFile = c.getCacheDir();
		//String cacheDir = cacheDirFile.getAbsolutePath();
		String dataDir = cacheDirFile.getParentFile().getAbsolutePath();
		String libDir = dataDir + "/lib";
		String tmpDir = System.getProperty("java.io.tmpdir") + "/ppsdk";
		File tmpDirFile = new File(tmpDir);
		tmpDirFile.mkdir();
		
		BreakpadUtil.registerBreakpad(tmpDirFile);
		
		PPBOX.libPath = libDir;
		//cacheDir.getAbsolutePath();
		PPBOX.logPath = tmpDir;
		PPBOX.logLevel = PPBOX.LEVEL_TRACE;
		PPBOX.load();
		PPBOX.StartEngine("161", "12", "111");
	}
	
	@SuppressLint("InlinedApi")
	public void open(String url)
	{
		capture = PPBOX.CaptureCreate("andriod", url);
		
		camera = Camera.open();
	    Camera.Parameters p = camera.getParameters();
	    p.setPreviewFormat(ImageFormat.NV21);
	    //List<int[]> fps_ranges = p.getSupportedPreviewFpsRange();
	    //p.setPreviewFpsRange(5000, 15000);
	    Camera.Size size = min_size(p.getSupportedPreviewSizes());
	    p.setPreviewSize(size.width, size.height);
	    camera.setParameters(p);
	    
		audio = get_audio_record();

		PPBOX.CaptureConfigData config = new PPBOX.CaptureConfigData();
		config.stream_count = 2;
		config.flags = 2; // multi_thread
		config.get_sample_buffers = null;
		config.free_sample = new PPBOX.FreeSampleCallBack() {
			@Override
			public boolean invoke(long context) {
				return PpboxStream.free_sample(context);
			}
		};
		
		PPBOX.CaptureInit(capture, config);
		
		video_stream = new PpboxStream(capture, 0, camera);
		
		audio_stream = new PpboxStream(capture, 1, audio);
		
		//audio_duration_ms = audio_info.__union3 * 1000 / audio_info.__union2;
		
		//samples = new Vector<PPBOX.Sample>();
	}
	
	public void preview(SurfaceHolder holder)
	{
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void start()
	{
		video_stream.start();
		final byte[] video_buffer = new byte[video_stream.buffer_size()];
		camera.addCallbackBuffer(video_buffer);
		
		audio_stream.start();
		
		camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
			private long start_time = System.nanoTime();
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				//System.out.println("video " + video_sample.decode_time);
				PpboxStream.InBuffer buffer = video_stream.pop();
				if (buffer == null) {
					System.out.println("video drop");
				} else {
					buffer.byte_buffer().put(data);
					long time = System.nanoTime() - start_time;
					//System.out.println("video put time = " + time / 1000 / 1000);
					video_stream.put(time / 1000, buffer);
				}
				camera.addCallbackBuffer(data);
			}
		});
		
		audio_thread = new Thread() {
            @Override
            public void run() {
            	audio_read_thread();
            }
		};
		audio_thread.setPriority(Thread.MAX_PRIORITY);
		audio_thread.start();
	}
	
	private void audio_read_thread()
	{
		audio.startRecording();
		int read_size = audio_stream.buffer_size();
		ByteBuffer drop_buffer = ByteBuffer.allocateDirect(read_size);
		while (!Thread.interrupted()) {
			PpboxStream.InBuffer buffer = audio_stream.pop();
			if (buffer == null) {
				System.out.println("audio drop");
				audio.read(drop_buffer, read_size);
				audio_stream.drop();
				continue;
			}
			int read = audio.read(buffer.byte_buffer(), read_size);
			//System.out.println("audio " + audio_write_index);
			if (read != read_size) {
				System.out.println("audio.read failed. read = " + read);
				break;
			}
			audio_stream.put(buffer);
			/*try {
				Thread.sleep(audio_duration_ms / 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
		audio.stop();
	}
		
	public void stop()
	{
		audio_thread.interrupt();
		try {
			audio_thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		camera.setPreviewCallbackWithBuffer(null);
	}
	
	public void close()
	{
		camera.stopPreview();
		camera.release();
		
		audio.release();
		
		PPBOX.CaptureDestroy(capture);
		
		//samples = null;
		
		video_stream = null;
		audio_stream = null;
	}
	
	private static int[] sampleRates = 
			new int[] { 8000, 11025, 22050, 44100 };
	private static short[] channelConfigs = 
			new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
	private static short[] audioFormats = 
			new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT };
	
	private AudioRecord get_audio_record()
	{
	    for (int sampleRate : sampleRates) {
    		for (short channelConfig : channelConfigs) {
    	    	for (short audioFormat : audioFormats) {
	    			try {
	                    System.out.println("Attempting rate " + sampleRate + "Hz, channel: " + channelConfig + ", bits: "
	                            + audioFormat);
	                    int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
	
	                    if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
	                    	continue;
	                    }
	                    bufferSize = PpboxStream.frame_size(1024, channelConfig, audioFormat) * 16;
                        // check if we can instantiate and have a success
                        AudioRecord recorder = new AudioRecord(
                        		AudioSource.DEFAULT, sampleRate, channelConfig, audioFormat, bufferSize);

                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        	System.out.println("Supported. buffer size: " + bufferSize);
                        	return recorder;
                        }
                        recorder.release();
	                } catch (Exception e) {
	                	System.out.println("Exception, keep trying." + e);
	                }
	            }
	    	}
        }
	    return null;
	}
	
	private Camera.Size min_size(List<Camera.Size> sizes)
	{
		Camera.Size ms = sizes.get(0);
		for (Camera.Size s : sizes) {
			if (s.width < ms.width || s.height < ms.height) {
				ms = s;
			}
		}
		return ms;
	}
}
