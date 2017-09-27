package com.example.ilona.flightsimprototype;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.widget.ImageView;

import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.sensors.internal.Vector3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;
import java.util.Vector;

//import static android.R.attr.x;
//import static android.R.attr.y;

/**
 * Created by Ilona on 19-Sep-17.
 */

public class Terrain{
    private int VERTEX_COUNT = 256;
    private int SIZE = 800;
    private int MAX_HEIGHT = 80;
    private int MAX_PIXEL_COLOR = 256 * 256 * 256;

    private FloatBuffer floorVertices;
    private FloatBuffer floorColors;
    private FloatBuffer floorNormals;
    private ShortBuffer floorIndices;

    public int count;
    public float[] vertices;
    public float[] normals;
    public float[] colors;
    public short[] indices;

    private int floorProgram;

    private int floorPositionParam;
    private int floorNormalParam;
    private int floorColorParam;
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;

    private float[] modelFloor;

    private float floorDepth = 20f;
    private int COORDS_PER_VERTEX = 3;

    private Bitmap bitmap;

    public Terrain(){
        bitmap = BitmapFactory.decodeResource(App.context().getResources(), R.drawable.heightmap);
        //VERTEX_COUNT = 256;
        modelFloor = new float[20];
       count = VERTEX_COUNT * VERTEX_COUNT;
        vertices = new float[count * 3];
        normals = new float[count * 3];
        colors = new float[count*4];
        indices = new short[6*(VERTEX_COUNT-1)*(VERTEX_COUNT-1)];

        //imageView = new ImageView(App.context());
        //imageView.setImageResource(App.context().getDrawable(R.drawable.heightmap));
        //Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        //bitmap = BitmapFactory.decodeResource(App.context().getResources(), R.drawable.heightmap);
        //Bitmap bitmap1 = Bitmap.createBitmap(R.drawable.heightmap, );
        //int pixel = bitmap.getPixel(bitmap.getWidth(),bitmap.getWidth());
        //generateFlatTerrain();
    }

    public void generateFlatTerrain(int vertexShader, int fragmentShader){
        int vertexPointer = 0;
        //VERTEX_COUNT = bitmap.getHeight();
        //int maxY = -20;
        //int minY = -80;
        for(int i=0;i<VERTEX_COUNT;i++){
            for(int j=0;j<VERTEX_COUNT;j++){
                //Random r = new Random();
                //int i1 = r.nextInt(maxY - minY) + minY;
                vertices[vertexPointer*3] = (float)j/((float)VERTEX_COUNT - 1) * SIZE - 200;
                //vertices[vertexPointer*3+1] = (float)i1;
                vertices[vertexPointer*3+1] = getHeight(j, i);
                //vertices[vertexPointer*3+1] = (float)0f;
                vertices[vertexPointer*3+2] = (float)i/((float)VERTEX_COUNT - 1) * SIZE -200;
                Vector3d normal = calcNormal(i, j);
                normals[vertexPointer*3] = (float)normal.x;
                normals[vertexPointer*3+1] = (float)normal.y;
                normals[vertexPointer*3+2] = (float)normal.z;
                colors[vertexPointer*4] = 1.0f;
                colors[vertexPointer*4+1] = 0.6523f;
                colors[vertexPointer*4+2] = 0.0f;
                colors[vertexPointer*4+3] = 1.0f;
                vertexPointer++;
            }
        }

        int pointer = 0;
        for(int gz=0;gz<VERTEX_COUNT-1;gz++){
            for(int gx=0;gx<VERTEX_COUNT-1;gx++){
                int topLeft = (gz*VERTEX_COUNT)+gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz+1)*VERTEX_COUNT)+gx;
                int bottomRight = bottomLeft + 1;
                indices[pointer++] = (short) topLeft;
                indices[pointer++] = (short) bottomLeft;
                indices[pointer++] = (short) topRight;
                indices[pointer++] = (short) topRight;
                indices[pointer++] = (short) bottomLeft;
                indices[pointer++] = (short) bottomRight;
            }
        }


        //ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COORDS.length * 4);
        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(vertices.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        floorVertices = bbFloorVertices.asFloatBuffer();
        floorVertices.put(vertices);
        floorVertices.position(0);

        //ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_NORMALS.length * 4);
        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(normals.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        floorNormals = bbFloorNormals.asFloatBuffer();
        floorNormals.put(normals);
        floorNormals.position(0);

        //ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COLORS.length * 4);
        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(colors.length * 4);
        bbFloorColors.order(ByteOrder.nativeOrder());
        floorColors = bbFloorColors.asFloatBuffer();
        floorColors.put(colors);
        floorColors.position(0);

        ByteBuffer bbFloorIndices = ByteBuffer.allocateDirect(indices.length * 2);
        bbFloorIndices.order(ByteOrder.nativeOrder());
        floorIndices = bbFloorIndices.asShortBuffer();
        floorIndices.put(indices);
        floorIndices.position(0);

        //int vertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        //int gridShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);

        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, vertexShader);
        GLES20.glAttachShader(floorProgram, fragmentShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        //checkGLError("Floor program");

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
        floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");

        //checkGLError("Floor program params");

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.

    }

    private float getHeight(int x, int z){
        if (x<0 || x>=bitmap.getHeight() || z<0 || z>= bitmap.getWidth()){
            return 0;
        }
        float height = bitmap.getPixel(x, z);
        height += MAX_PIXEL_COLOR/2f;
        height /= MAX_PIXEL_COLOR/2f;
        height *= MAX_HEIGHT;
        return height;
    }

    private Vector3d calcNormal(int x, int z){
        float heightL = getHeight(x-1, z);
        float heightR = getHeight(x+1, z);
        float heightD = getHeight(x, z-1);
        float heightU = getHeight(x, z+1);
        Vector3d normal = new Vector3d(heightL-heightR, 2f, heightD-heightU);
        normal.normalize();
        return normal;
    }

    /**
     * Draw the floor.
     *
     * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor(float[] lightPosInEyeSpace, float[] modelView, float[] modelViewProjection) {
        GLES20.glUseProgram(floorProgram);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
        GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false, modelViewProjection, 0);
        GLES20.glVertexAttribPointer(
                floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, floorVertices);
        GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0, floorNormals);
        GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floorColors);

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);
        GLES20.glEnableVertexAttribArray(floorColorParam);

        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 24);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, floorIndices);

        GLES20.glDisableVertexAttribArray(floorPositionParam);
        GLES20.glDisableVertexAttribArray(floorNormalParam);
        GLES20.glDisableVertexAttribArray(floorColorParam);

        //checkGLError("drawing floor");
    }

    public static int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

}
