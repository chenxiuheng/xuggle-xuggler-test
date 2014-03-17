package com.xuggle.transcode;

import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IContainer.Type;

public class Split {
	private static Logger logger = LoggerFactory.getLogger(Split.class);
	
	public static void main(String[] args) throws Exception {
		String src = "0001.flv";
		
		segmentMediaFile(src, "m3u8", "flv");
	}
	
	public static void segmentMediaFile (String sourceFile, String destinationDir, String extension) throws Exception {
	    IMediaReader reader = ToolFactory.makeReader(sourceFile);
	    
	    int segIndex = 1;
	    IMediaWriter writer = ToolFactory.makeWriter(destinationDir + "/_" + segIndex + "."  + extension , reader);
	    
	    reader.addListener(writer);
	    reader.addListener(ToolFactory.makeViewer(true));

	    long preEndTime = System.currentTimeMillis() / 1000;
	    while(reader.readPacket() == null) {
	    	long now = System.currentTimeMillis() / 1000;
	    	
	    	if (now - preEndTime > 10) {
	    		preEndTime = now;
	    		
	    		//writer.flush();
	    		//writer.close();
	    		IMediaWriter oldWriter = writer;
	    		reader.removeListener(oldWriter);
	    		
	    		segIndex ++;
	    	    writer = ToolFactory.makeWriter(destinationDir + "/_" + segIndex + "."  + extension , reader);
	    		reader.addListener(writer);
	    		
	    		oldWriter.close();
	    	}
	    }
	}


	private static IContainer getOutputContainer (final int counter, IContainer input, String destinationDir, String extension) throws Exception {
	    IContainer output = IContainer.make();
	    output.open(destinationDir + "/segment_" + counter + "." + extension, IContainer.Type.WRITE, null);

	    int numStreams = input.getNumStreams();
	    for(int i = 0; i < numStreams; i++)
	    {
	      final IStream stream = input.getStream(i);
	      final IStreamCoder coder = stream.getStreamCoder();
	      coder.open();

	      IStreamCoder newCoder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, coder);
	        if(newCoder != null ){
	          output.addNewStream(i);
	          output.getStream(i).setStreamCoder(newCoder);
	          newCoder.open();
	      } else {
	          logger.warn("Is there an invalid stream present in video file: " + input.getURL() + "! IGNORING, but this might be serious");
	      }


	    }

	    // write the output header
	    writeHeader(output);

	    return output;
	}

	private static void writeHeader(IContainer output){
	    output.writeHeader();
	}

	private static void writeTrailer(IContainer output){
	    // write the output trailers
	    output.writeTrailer();
	    int streams = output.getNumStreams();
	    for(int j = 0; j < streams; j++)
	    {
	      output.getStream(j).getStreamCoder().close();
	    }
	    output.close();
	}
}
