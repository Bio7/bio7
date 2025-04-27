package ij.plugin.filter;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.Animator;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
This plugin implements the File/Save As/AVI command.
Supported formats:
  Uncompressed 8-bit (gray or indexed color), 24-bit (RGB),
  JPEG and PNG compression of individual frames
  16-bit and 32-bit (float) images are converted to 8-bit
The plugin is based on the FileAvi class written by William Gandler,
part of Matthew J. McAuliffe's MIPAV program, available from
http://mipav.cit.nih.gov/.
2008-06-05: Support for jpeg and png-compressed output and
composite images by Michael Schmid.
2015-09-28: Writes AVI 2.0 if the file size would be above approx. 0.9 GB

* The AVI format written looks like this:
* RIFF AVI            RIFF HEADER, AVI CHUNK					
*   | LIST hdrl       MAIN AVI HEADER
*   | | avih          AVI HEADER
*   | | LIST strl     STREAM LIST(s) (One per stream)
*   | | | strh        STREAM HEADER (Required after above; fourcc type is 'vids' for video stream)
*   | | | strf        STREAM FORMAT (for video: BitMapInfo; may also contain palette)
*   | | | strn        STREAM NAME
*   | | | indx        MAIN 'AVI 2.0' INDEX of 'ix00' indices
*   | LIST movi       MOVIE DATA (maximum approx. 0.95 GB)
*   | | 00db or 00dc  FRAME (b=uncompressed, c=compressed)
*   | | 00db or 00dc  FRAME
*   | | ...
*   | | ix00          AVI 2.0-style index of frames within this 'movi' list
* RIFF AVIX	          Only if required by size (this is AVI 2.0 extension)
*   | LIST movi       MOVIE DATA (maximum approx. 0.95 GB)
*   | | 00db or 00dc  FRAME
*   | | ...
*   | | ix00          AVI 2.0-style index of frames within this 'movi' list
* RIFF AVIX	          further chunks, each approx 0.95 GB (AVI 2.0)
* ...
*/

public class AVI_Writer implements PlugInFilter {
    //four-character codes for compression
    // Note: byte sequence in four-cc is reversed - ints in Intel (little endian) byte order.
    // Note that compression codes BI_JPEG=4 and BI_PNG=5 are not understood by avi players
    // (even not by MediaPlayer, even though these codes are specified by Microsoft).
    public final static int  NO_COMPRESSION   = 0;            //no compression, also named BITMAPINFO.BI_RGB
    public final static int  JPEG_COMPRESSION = 0x47504a4d;   //'MJPG' JPEG compression of individual frames
    public final static int  PNG_COMPRESSION  = 0x20676e70;   //'png ' PNG compression of individual frames
    private final static int FOURCC_00db = 0x62643030;        //'00db' uncompressed frame
    private final static int FOURCC_00dc = 0x63643030;        //'00dc' compressed frame
    private final static int MAX_INDX_SIZE = 3072;            //max length of index of indices 'indx'
    private final static int JUNK_SIZE_THRESHOLD = 950*1024*1024;   //if size exceeds this, makes a new RIFF AVIX chunk
    //compression options: dialog parameters
    private int      compressionIndex = 2; //0=none, 1=PNG, 2=JPEG
    private static int      jpegQuality = 90;    //0 is worst, 100 best (not currently used)
    private final static String[] COMPRESSION_STRINGS = new String[] {"None", "PNG", "JPEG"};
    private final static int[] COMPRESSION_TYPES = new int[] {NO_COMPRESSION, PNG_COMPRESSION, JPEG_COMPRESSION};

    private ImagePlus imp;
    private RandomAccessFile raFile;
    private int             xDim,yDim;      //image size
    private int             zDim;           //number of movie frames (stack size)
    private int             bytesPerPixel;  //8 or 24
    private int             frameDataSize;  //in bytes (uncompressed)
    private int             biCompression;  //compression type (0, 'JPEG, 'PNG')
    private int             linePad;        //no. of bytes to add for padding of data lines to 4*n length
    private byte[]          bufferWrite;    //output buffer for image data
    private BufferedImage   bufferedImage;  //data source for writing compressed images
    private RaOutputStream  raOutputStream; //output stream for writing compressed images
    private long[]          sizePointers =  //a stack of the pointers to the chunk sizes (pointers are
                                new long[5];//  remembered to write the sizes later, when they are known)
    private int             stackPointer;   //points to first free position in sizePointers stack
    private int             endHeadPointer; //position of first 'movi' chunk, i.e., end of the space reserved for indx
    // AVI-2 related:
    private long            pointer2indx;   //points to main index-of-indices 'indx'
    private int             nIndxEntries=0; //number of 'indx' entries
    private long            pointer2indxNEntriesInUse;  //points to 'nEntriesInUse' of 'indx'
    private long            pointer2indxNextEntry;   //points to next free slot of 'indx'

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL+NO_CHANGES;
    }

    /** Asks for the compression type and filename; then saves as AVI file */
    public void run(ImageProcessor ip) {
        if (!showDialog(imp)) return;          //compression type dialog
        SaveDialog sd = new SaveDialog("Save as AVI...", imp.getTitle(), ".avi");
        String fileName = sd.getFileName();
        if (fileName == null)
            return;
        String fileDir = sd.getDirectory();
        FileInfo fi = imp.getOriginalFileInfo();
        if (fi!=null && imp.getStack().isVirtual() && fileDir.equals(fi.directory) && fileName.equals(fi.fileName)) {
            IJ.error("AVI Writer", "Virtual stacks cannot be saved in place.");
            return;
        }
        try {
            writeImage(imp, fileDir + fileName, COMPRESSION_TYPES[compressionIndex], jpegQuality);
            IJ.showStatus("");
        } catch (IOException e) {
            IJ.error("AVI Writer", "An error occured writing the file.\n \n" + e);
        }
        IJ.showStatus("");
    }

    private boolean showDialog(ImagePlus imp) {
    	String options = Macro.getOptions();
    	if (options!=null) {
    		if (!options.contains("compression="))
    			options = "compression=JPEG "+options;
    		options = options.replace("compression=Uncompressed", "compression=None");
    		Macro.setOptions(options);
    	}
  		double fps = getFrameRate(imp);
 		int decimalPlaces = (int) fps == fps?0:1;
        GenericDialog gd = new GenericDialog("Save as AVI...");
        gd.addChoice("Compression:", COMPRESSION_STRINGS, COMPRESSION_STRINGS[compressionIndex]);
		gd.addNumericField("Frame Rate:", fps, decimalPlaces, 3, "fps");
        gd.showDialog();                            // user input (or reading from macro) happens here
        if (gd.wasCanceled())                       // dialog cancelled?
            return false;
        compressionIndex = gd.getNextChoiceIndex();
        fps = gd.getNextNumber();
        if (fps<=0.5) fps = 0.5;
		imp.getCalibration().fps = fps;
		return true;
    }

    /** Writes an ImagePlus (stack) as AVI file. */
    public void writeImage (ImagePlus imp, String path, int compression, int jpegQuality)
            throws IOException {
        if (compression!=NO_COMPRESSION && compression!=JPEG_COMPRESSION && compression!=PNG_COMPRESSION)
            throw new IllegalArgumentException("Unsupported Compression 0x"+Integer.toHexString(compression));
        this.biCompression = compression;
        if (jpegQuality < 0) jpegQuality = 0;
        if (jpegQuality > 100) jpegQuality = 100;
        this.jpegQuality = jpegQuality;
        File file = new File(path);
        raFile = new RandomAccessFile(file, "rw");
        raFile.setLength(0);
        imp.startTiming();

        //  G e t   s t a c k   p r o p e r t i e s
        boolean isComposite = imp.isComposite();
        boolean isHyperstack = imp.isHyperStack();
        boolean isOverlay = imp.getOverlay()!=null && !imp.getHideOverlay();
        xDim = imp.getWidth();   //image width
        yDim = imp.getHeight();   //image height
        zDim = imp.getStackSize(); //number of frames in video
		boolean saveFrames=false, saveSlices=false, saveChannels=false;
        int channels = imp.getNChannels();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();
		int channel = imp.getChannel();
		int slice = imp.getSlice();
		int frame = imp.getFrame();
		if (isHyperstack || isComposite) {
			if (frames>1) {
				saveFrames = true;
				zDim = frames;
			} else if (slices>1) {
				saveSlices = true;
				zDim = slices;
			} else if (channels>1) {
				saveChannels = true;
				zDim = channels;
			} else
				isHyperstack = false;
		}

        if (imp.getType()==ImagePlus.COLOR_RGB || isComposite || biCompression==JPEG_COMPRESSION || isOverlay)
            bytesPerPixel = 3;  //color and JPEG-compressed files
        else
            bytesPerPixel = 1;  //gray 8, 16, 32 bit and indexed color: all written as 8 bit
        boolean writeLUT = bytesPerPixel==1; // QuickTime reads the avi palette also for PNG
        linePad = 0;
        int minLineLength = bytesPerPixel*xDim;
        if (biCompression==NO_COMPRESSION && minLineLength%4!=0)
            linePad = 4 - minLineLength%4; //uncompressed lines written must be a multiple of 4 bytes
        frameDataSize = (bytesPerPixel*xDim+linePad)*yDim;
        int microSecPerFrame = (int)Math.round((1.0/getFrameRate(imp))*1.0e6);
        int dwChunkId = biCompression==NO_COMPRESSION ? FOURCC_00db : FOURCC_00dc;
        long sizeEstimate = bytesPerPixel*xDim*yDim*(long)zDim;
        //boolean writeAVI2index = true;//frameDataSize*zDim > 1000000000;
        int nAvixChunksEstimate = (int)(sizeEstimate/JUNK_SIZE_THRESHOLD);  //estimated number of AVIX junks
        endHeadPointer = 4096+((nAvixChunksEstimate*16+1000)/1024)*1024;    //reserve plenty of space for 'indx'

        //  W r i t e   A V I   f i l e   h e a d e r
        writeString("RIFF");    // signature
        chunkSizeHere();        // size of file (nesting level 0)
        writeString("AVI ");    // RIFF type
        writeString("LIST");    // first LIST chunk, which contains information on data decoding
        chunkSizeHere();        // size of LIST (nesting level 1)
        writeString("hdrl");    // LIST chunk type
        writeString("avih");    // Write the avih sub-CHUNK
        writeInt(0x38);         // length of the avih sub-CHUNK (38H) not including the
                                // the first 8 bytes for avihSignature and the length
        writeInt(microSecPerFrame); // dwMicroSecPerFrame - Write the microseconds per frame
        writeInt(0);            // dwMaxBytesPerSec (maximum data rate of the file in bytes per second)
        writeInt(0);            // dwPaddingGranularity (for header length?), previously dwReserved1, usually set to zero.
        writeInt(0x10);         // dwFlags - just set the bit for AVIF_HASINDEX
                                //   10H AVIF_HASINDEX: The AVI file has an idx1 chunk containing
                                //   an index at the end of the file.  For good performance, all
                                //   AVI files should contain an index.
        writeInt(zDim);         // dwTotalFrames - total frame number
        writeInt(0);            // dwInitialFrames -Initial frame for interleaved files.
                                // Noninterleaved files should specify 0.
        writeInt(1);            // dwStreams - number of streams in the file - here 1 video and zero audio.
        writeInt(0);      // dwSuggestedBufferSize 
         writeInt(xDim);         // dwWidth - image width in pixels
        writeInt(yDim);         // dwHeight - image height in pixels
        writeInt(0);            // dwReserved[4]
        writeInt(0);
        writeInt(0);
        writeInt(0);

        //  W r i t e   s t r e a m   i n f o r m a t i o n
        writeString("LIST");    // List of stream headers
        chunkSizeHere();        // size of LIST (nesting level 2)
        writeString("strl");    // LIST chunk type: stream list
        writeString("strh");    // stream header 
        writeInt(56);           // Write the length of the strh sub-CHUNK
        writeString("vids");    // fccType - type of data stream - here 'vids' for video stream
        writeString("DIB ");    // 'DIB ' for Microsoft Device Independent Bitmap.
        writeInt(0);            // dwFlags
        writeInt(0);            // wPriority, wLanguage
        writeInt(0);            // dwInitialFrames
        writeInt(1);            // dwScale
        writeInt((int)Math.round(getFrameRate(imp))); //  dwRate - frame rate for video streams
        writeInt(0);            // dwStart - this field is usually set to zero
        writeInt(zDim);         // dwLength - playing time of AVI file as defined by scale and rate
                                // Set equal to the number of frames
        writeInt(0);            // dwSuggestedBufferSize for reading the stream.
                                // Typically, this contains a value corresponding to the largest chunk
                                // in a stream.
        writeInt(-1);           // dwQuality - encoding quality given by an integer between
                                // 0 and 10,000.  If set to -1, drivers use the default
                                // quality value.
        writeInt(0);            // dwSampleSize. 0 means that each frame is in its own chunk
        writeShort((short)0);   // left of rcFrame if stream has a different size than dwWidth*dwHeight(unused)
        writeShort((short)0);   // top
        writeShort((short)0);   // right
        writeShort((short)0);   // bottom
        // end of 'strh' chunk, stream format follows
        writeString("strf");    // stream format chunk
        chunkSizeHere();        // size of 'strf' chunk (nesting level 3)
        writeInt(40);           // biSize - Write header size of BITMAPINFO header structure
                                // Applications should use this size to determine which BITMAPINFO header structure is
                                // being used.  This size includes this biSize field.
        writeInt(xDim);         // biWidth - width in pixels
        writeInt(yDim);         // biHeight - image height in pixels. (May be negative for uncompressed
                                // video to indicate vertical flip).
        writeShort(1);          // biPlanes - number of color planes in which the data is stored
        writeShort((short)(8*bytesPerPixel)); // biBitCount - number of bits per pixel #
        writeInt(biCompression); // biCompression - type of compression used (uncompressed: NO_COMPRESSION=0)
        int biSizeImage =       // Image Buffer. Quicktime needs 3 bytes also for 8-bit png
                (biCompression==NO_COMPRESSION)?0:xDim*yDim*bytesPerPixel;
        writeInt(biSizeImage);  // biSizeImage (buffer size for decompressed mage) may be 0 for uncompressed data
        writeInt(0);            // biXPelsPerMeter - horizontal resolution in pixels per meter
        writeInt(0);            // biYPelsPerMeter - vertical resolution in pixels per meter
        writeInt(writeLUT ? 256:0); // biClrUsed (color table size; for 8-bit only)
        writeInt(0);            // biClrImportant - specifies that the first x colors of the color table
                                // are important to the DIB.  If the rest of the colors are not available,
                                // the image still retains its meaning in an acceptable manner.  When this
                                // field is set to zero, all the colors are important, or, rather, their
                                // relative importance has not been computed.
        if (writeLUT)           // write color lookup table
            writeLUT(imp.getProcessor());
        chunkEndWriteSize();    //'strf' chunk finished (nesting level 3)
        
        writeString("strn");    // Use 'strn' to provide a zero terminated text string describing the stream
        writeInt(16);           // length of the strn sub-CHUNK (must be even)
        writeString("ImageJ AVI     \0"); //must be 16 bytes as given above (including the terminating 0 byte)
        pointer2indx = raFile.getFilePointer();
        writeString("indx");    // 'indx' chunk type: Index of indices
        chunkSizeHere();        // size of 'indx' (nesting level 3)
        writeShort(4);          // wLongsPerEntry = 4 ('Longs' are 32-bit here!)
        writeByte(0);           // bIndexSubType=0
        writeByte(0);           // bIndexType=0: AVI_INDEX_OF_INDEXES
        pointer2indxNEntriesInUse = raFile.getFilePointer();
        writeInt(0);            // nEntriesInUse, will be filled in later
        writeInt(dwChunkId);    // dwChunkId, '00dc' or '00db'
        writeInt(0); writeInt(0); writeInt(0); // dwReserved[3]
        pointer2indxNextEntry = raFile.getFilePointer();
        chunkEndWriteSize();    //'indx' chunk finished (nesting level 3), will be modified by writeMainIndxEntry
        writeString("JUNK");    // write a JUNK chunk for padding (will be moved and shortened by writeMainIndxEntry)
        chunkSizeHere();        // size of 'JUNK' for padding (nesting level 3)
        raFile.seek(endHeadPointer);      // we continue here
        chunkEndWriteSize();    // 'JUNK' finished (nesting level 3)
        chunkEndWriteSize();    // LIST 'strl' finished (nesting level 2)
        chunkEndWriteSize();    // LIST 'hdrl' finished (nesting level 1)

        //  P r e p a r e   f o r   w r i t i n g   d a t a
        if (biCompression == NO_COMPRESSION)
            bufferWrite = new byte[frameDataSize];
        else
            raOutputStream = new RaOutputStream(raFile); //needed for writing compressed formats
        //int maxChunkLength = 0;                 // needed for dwSuggestedBufferSize
        int[] dataChunkOffset = new int[zDim];  // remember chunk positions...
        int[] dataChunkLength = new int[zDim];  // ... and sizes for the index

        int currentFilePart = 0;// 0 is inside RIFF AVI (AVI 1.0 compatible), >0 is RIFF AVIX (data chunk of AVI 2.0)

        //  W r i t e   f r a m e   d a t a   a n d   i n d i c e s
        boolean writeAVI2index = false; // see whether we need an AVI2 index (large files only)
        int iFrame = 0;
        while (iFrame < zDim) {
            if (currentFilePart > 0) {  // open new RIFF AVIX chunk
                writeString("RIFF");
                chunkSizeHere();        // size of chunk (nesting level 0)
                writeString("AVIX");    // RIFF type
                //IJ.log("AVIX starts at iFrame="+iFrame);
            }
            writeString("LIST");        // this LIST chunk contains the AVI-2 style index and the actual data
            chunkSizeHere();            // size of LIST (nesting level 1)
            long moviPointer = raFile.getFilePointer();
            writeString("movi");        // write LIST type 'movi'

            int firstFrameInChunk = iFrame;

            //   W r i t e   s i n g l e   f r a m e
            while (iFrame<zDim) {
                if (iFrame %10==0) {
                    IJ.showProgress(iFrame, zDim);
                    IJ.showStatus(iFrame+"/"+zDim);
                }
                ImageProcessor ip = null;      // get the image to write ...
                if (isComposite || isHyperstack || isOverlay) {
                    if (saveFrames)
                        imp.setPositionWithoutUpdate(channel, slice, iFrame+1);
                    else if (saveSlices)
                        imp.setPositionWithoutUpdate(channel, iFrame+1, frame);
                    else if (saveChannels)
                        imp.setPositionWithoutUpdate(iFrame+1, slice, frame);
                    ImagePlus imp2 = imp;
                    if (isOverlay) {
                        if (!(saveFrames||saveSlices||saveChannels))
                            imp.setSliceWithoutUpdate(iFrame+1);
                        imp2 = imp.flatten();
                    }
                    ip = new ColorProcessor(imp2.getImage());
                } else
                    ip = zDim==1 ? imp.getProcessor() : imp.getStack().getProcessor(iFrame+1);
                int chunkPointer = (int)raFile.getFilePointer();
                writeInt(dwChunkId);            // start writing chunk: '00db' or '00dc'
                chunkSizeHere();                // size of '00db' or '00dc' chunk (nesting level 2)
                if (biCompression == NO_COMPRESSION) {
                    if (bytesPerPixel==1)
                        writeByteFrame(ip);
                    else
                        writeRGBFrame(ip);
                } else
                    writeCompressedFrame(ip);
                dataChunkOffset[iFrame] = (int)(chunkPointer - moviPointer);
                dataChunkLength[iFrame] = (int)(raFile.getFilePointer() - chunkPointer - 8); //size excludes '00db' and size fields
                chunkEndWriteSize();            // '00db' or '00dc' chunk finished (nesting level 2)
                //if (IJ.escapePressed()) {
                //    IJ.showStatus("Save as Avi INTERRUPTED");
                //    break;
                //}
                iFrame++;
                if (raFile.getFilePointer() - moviPointer > JUNK_SIZE_THRESHOLD)
                    break;                      // make sure we don't get over 1GB
            } // while (iFrame<zDim)
            int nFramesInChunk = iFrame - firstFrameInChunk;

            //  W r i t e   A V I - 2   I n d e x
            if (iFrame < zDim)
                writeAVI2index = true;      //can't write everything the first time? Then we need the AVI 2 format.
            if (writeAVI2index) {
                long ix00pointer = raFile.getFilePointer();
                writeString("ix00");        // AVI 2.0 style index of frames within the chunk
                chunkSizeHere();            // size of ix00 chunk (nesting level 2)
                writeShort(2);              // wLongsPerEntry = 2 ('Longs' are 32-bit here!)
                writeByte(0);               // bIndexSubType=0
                writeByte(1);               // bIndexType=1: AVI_INDEX_OF_CHUNKS
                writeInt(nFramesInChunk);   // nEntriesInUse
                writeInt(dwChunkId);        // dwChunkId, '00dc' or '00db'
                writeLong(moviPointer);     // qwBaseOffset
                writeInt(0);                // dwReserved, first two are qwBaseOffset?
                for (int z=firstFrameInChunk; z<iFrame; z++) {
                    writeInt(dataChunkOffset[z]+8); //note: AVI--2 index points to chunk data, not chunk header
                    writeInt(dataChunkLength[z]);   //length without chunk header
                }
                //IJ.log("write ix00: frames "+firstFrameInChunk+"-"+(iFrame-1)+" offset "+Long.toHexString(dataChunkOffset[firstFrameInChunk])+"-"+Long.toHexString(dataChunkOffset[iFrame-1]));
                //enter this ix00 index to index of indices:
                writeMainIndxEntry(ix00pointer, (int)(raFile.getFilePointer()-ix00pointer), nFramesInChunk);

                chunkEndWriteSize();        // 'ix00' finished (nesting level 2)
            }
            chunkEndWriteSize();        // LIST 'movi' finished (nesting level 1)

            //  W r i t e   A V I - 1   I n d e x
            if (currentFilePart == 0) {
                writeString("idx1");    // Write the idx1 chunk
                chunkSizeHere();        // size of 'idx1' chunk (nesting level 1)
                for (int z = 0; z < iFrame; z++) {
                    writeInt(dwChunkId);// ckid field: '00db' or '00dc'
                    writeInt(0x10);     // flags: select AVIIF_KEYFRAME
                                 // AVIIF_KEYFRAME 0x00000010
                                 // The flag indicates key frames in the video sequence.
                                 // Key frames do not need previous video information to be decompressed.
                                 // AVIIF_NOTIME 0x00000100 The CHUNK does not influence video timing (for
                                 //   example a palette change CHUNK).
                                 // AVIIF_LIST 0x00000001 marks a LIST CHUNK.
                                 // AVIIF_TWOCC 2L
                                 // AVIIF_COMPUSE 0x0FFF0000 These bits are for compressor use.
                     writeInt(dataChunkOffset[z]); // offset to the chunk header (not data)
                                 // offset can be relative to file start or 'movi'
                     writeInt(dataChunkLength[z]); // length without chunk header
                }  // for (z = 0; z < zDim; z++)
                chunkEndWriteSize();    // 'idx1' finished (nesting level 1)
            }
            chunkEndWriteSize();    // 'RIFF' File finished (nesting level 0)
            currentFilePart++;
        } //while (iFrame < zDim)

        if (!writeAVI2index) {      //delete main AVI 2 index prepared previously
            raFile.seek(pointer2indx);
            writeString("JUNK");        // overwrite 'indx'
            chunkSizeHere();            // size of 'JUNK' for padding goes here
            raFile.seek(endHeadPointer);// end of the padded range
            chunkEndWriteSize();        // 'JUNK' finished              
        }

        raFile.close();
        IJ.showProgress(1.0);
		if (isComposite || isHyperstack)
			imp.setPosition(channel, slice, frame);
    }

    /** Reserve space to write the size of chunk and remember the position
     *  for a later call to chunkEndWriteSize().
     *  Several levels of chunkSizeHere() and chunkEndWriteSize() may be nested.
     */
    private void chunkSizeHere() throws IOException {
        sizePointers[stackPointer] = raFile.getFilePointer();
        writeInt(0);    //for now, write 0 to reserve space for "size" item
        stackPointer++;
    }
    
    /** At the end of a chunk, calculate its size and write it to the
     *  position remembered previously. Also pads to 2-byte boundaries.
     */
    private void chunkEndWriteSize() throws IOException {
        stackPointer--;
        long position = raFile.getFilePointer();
        raFile.seek(sizePointers[stackPointer]);
        writeInt((int)(position - (sizePointers[stackPointer]+4)));
        raFile.seek(((position+1)/2)*2);    //pad to 2-byte boundary
        //IJ.log("chunk at 0x"+Long.toHexString(sizePointers[stackPointer]-4)+"-0x"+Long.toHexString(position));
    }

    /** Enter a local index 'ix00' to 'indx', the index of indices */
    private void writeMainIndxEntry(long ix00pointer, int dwSize, int nFrames) throws IOException {
        if (pointer2indxNextEntry + 16 + 8 > MAX_INDX_SIZE) {
            raFile.close();
            throw new RuntimeException("AVI_Writer ERROR: Index Size Overflow");
        }
        long savePosition = raFile.getFilePointer();
        raFile.seek(pointer2indxNextEntry);
        writeLong(ix00pointer);
        writeInt(dwSize);
        writeInt(nFrames);
        pointer2indxNextEntry += 16;
        nIndxEntries++;
        writeString("JUNK");        // write a JUNK chunk for padding
        chunkSizeHere();            // size of 'JUNK' for padding goes here
        raFile.seek(endHeadPointer);// end of the padded range
        chunkEndWriteSize();        // 'JUNK' finished (nesting level 3)
        raFile.seek(pointer2indx+4);
        writeInt((int)(pointer2indxNextEntry - pointer2indx - 8)); //write new size of 'indx'
        raFile.seek(pointer2indxNEntriesInUse);
        writeInt(nIndxEntries);     //write new number of 'indx' entries
        raFile.seek(savePosition);
    }

    /** Write Grayscale (or indexed color) data. Lines are  
     *  padded to a length that is a multiple of 4 bytes. */
    private void writeByteFrame(ImageProcessor ip) throws IOException {
        ip = ip.convertToByte(true);
        byte[] pixels = (byte[])ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        int c, offset, index = 0;
        for (int y=height-1; y>=0; y--) {
            offset = y*width;
            for (int x=0; x<width; x++)
                bufferWrite[index++] = pixels[offset++];
            for (int i = 0; i<linePad; i++)
                bufferWrite[index++] = (byte)0;
        }
        raFile.write(bufferWrite);
    }

    /** Write RGB data. Each 3-byte triplet in the bitmap array represents
     *  blue, green, and red, respectively, for a pixel.  The color bytes are
     *  in reverse order (Windows convention). Lines are padded to a length
     *  that is a multiple of 4 bytes. */
    private void writeRGBFrame(ImageProcessor ip) throws IOException {
        ip = ip.convertToRGB();
        int[] pixels = (int[])ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        int c, offset, index = 0;
        for (int y=height-1; y>=0; y--) {
            offset = y*width;
            for (int x=0; x<width; x++) {
                c = pixels[offset++];
                bufferWrite[index++] = (byte)(c&0xff); // blue
                bufferWrite[index++] = (byte)((c&0xff00)>>8); //green
                bufferWrite[index++] = (byte)((c&0xff0000)>>16); // red
            }
            for (int i = 0; i<linePad; i++)
                bufferWrite[index++] = (byte)0;
        }
        raFile.write(bufferWrite);
    }

    /** Write a frame as jpeg- or png-compressed image */
	private void writeCompressedFrame(ImageProcessor ip) throws IOException {
		//IJ.log("BufferdImage Type="+bufferedImage.getType()); // 1=RGB, 13=indexed
		if (biCompression==JPEG_COMPRESSION) {
			BufferedImage bi = getBufferedImage(ip);
			ImageIO.write(bi, "jpeg", raOutputStream);
		} else { //if (biCompression==PNG_COMPRESSION) {
			BufferedImage bi = ip.getBufferedImage();
			ImageIO.write(bi, "png", raOutputStream);
		}
	}

	private BufferedImage getBufferedImage(ImageProcessor ip) {
		BufferedImage bi = new BufferedImage(ip.getWidth(), ip.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.drawImage(ip.createImage(), 0, 0, null);
		return bi;
	}

    /** Write the color table entries (for 8 bit grayscale or indexed color).
     *  Byte order or LUT entries: blue byte, green byte, red byte, 0 byte */
    private void writeLUT(ImageProcessor ip) throws IOException {
        IndexColorModel cm = (IndexColorModel)(ip.getCurrentColorModel());
        int mapSize = cm.getMapSize();
        byte[] lutWrite = new byte[4*256];
        for (int i = 0; i<256; i++) {
            if (i<mapSize) {
                lutWrite[4*i] = (byte)cm.getBlue(i);
                lutWrite[4*i+1] = (byte)cm.getGreen(i);
                lutWrite[4*i+2] = (byte)cm.getRed(i);
                lutWrite[4*i+3] = (byte)0;
            }
        }
        raFile.write(lutWrite);
    }

    private double getFrameRate(ImagePlus imp) {
        double rate = imp.getCalibration().fps;
        if (rate==0.0)
            rate = Animator.getFrameRate();
        if (rate<=0.5) rate = 0.5;
        //if (rate>60.0) rate = 60.0;
        return rate;
    }

    private void writeString(String s) throws IOException {
        byte[] bytes =  s.getBytes("UTF8");
        raFile.write(bytes);
    }

    /** Write 8-byte int with Intel (little-endian) byte order
     * (note: RandomAccessFile.writeInt has other byte order than AVI) */
    private void writeLong(long v) throws IOException {
        for (int i=0; i<8; i++) {
            raFile.write((int)(v & 0xFFL));
            v = v>>>8;
        }
        //IJ.log("long: 0x"+Long.toHexString(v)+"="+v);
    }

    /** Write 4-byte int with Intel (little-endian) byte order
     * (note: RandomAccessFile.writeInt has other byte order than AVI) */
    private void writeInt(int v) throws IOException {
        raFile.write(v & 0xFF);
        raFile.write((v >>>  8) & 0xFF);
        raFile.write((v >>> 16) & 0xFF);
        raFile.write((v >>> 24) & 0xFF);
        //IJ.log("int: 0x"+Integer.toHexString(v)+"="+v);
    }

    /** Write 2-byte short with Intel (little-endian) byte order
     * (note: RandomAccessFile.writeShort has other byte order than AVI) */
    private void writeShort(int v) throws IOException {
        raFile.write(v & 0xFF);
        raFile.write((v >>> 8) & 0xFF);
    }

    /** Write a byte */
    private void writeByte(int v) throws IOException {
        raFile.write(v & 0xFF);
    }

    /** An output stream directed to a RandomAccessFile (starting at the current position) */
    class RaOutputStream extends OutputStream {
        RandomAccessFile raFile;
        RaOutputStream (RandomAccessFile raFile) {
            this.raFile = raFile;
        }
        public void write (int b) throws IOException {
            //IJ.log("stream: byte");
            raFile.writeByte(b); //just for completeness, usually not used by image encoders
        }
        public void write (byte[] b) throws IOException {
            //IJ.log("stream: array len="+b.length);
            raFile.write(b);
        }
        public void write (byte[] b, int off, int len) throws IOException {
            //IJ.log("stream: array="+b.length+" off="+off+" len="+len);
            raFile.write(b, off, len);
        }
    }

}
