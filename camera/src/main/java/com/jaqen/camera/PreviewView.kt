package com.jaqen.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import java.lang.RuntimeException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PreviewView: GLSurfaceView, GLSurfaceView.Renderer{
    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var drawer: GLDrawer? = null
    private var resolution: Size? = null
    //private var listener: PreviewListener? = null
    private var onCreate: (()->Unit) = {}

    constructor(context: Context): super(context){

    }

    constructor(context: Context, attributes: AttributeSet): super(context, attributes){

    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);//设置清屏颜色

        textureId = createOESTextureObject()
        drawer = GLDrawer(textureId)

        onCreate()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height);//设置视口尺寸
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        surfaceTexture?.apply {
            val mtx = FloatArray(16)

            updateTexImage()
            getTransformMatrix(mtx)

            drawer?.draw(mtx, resolution)
        }
    }

    val surfaceProvider = Preview.SurfaceProvider {
        if (textureId == 0)
            throw RuntimeException("在PreviewView创建完成前启用了相机")

        surfaceTexture = SurfaceTexture(textureId).apply {
            setDefaultBufferSize(it.resolution.width, it.resolution.height)
            setOnFrameAvailableListener {
                requestRender()
            }
        }
        //surfaceTexture?.setDefaultBufferSize(it.resolution.width, it.resolution.height)
        resolution = it.resolution

        it.provideSurface(Surface(surfaceTexture),
            ContextCompat.getMainExecutor(context)){ result ->

            if (result.resultCode == SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY){
                drawer = null

                surfaceTexture?.release()
                surfaceTexture = null
            }
        }
    }

    private fun createOESTextureObject(): Int {
        val tex = IntArray(1)

        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0)
        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        //设置纹理过滤参数
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        //解除纹理绑定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        return tex[0]
    }

    fun onPreviewCreated(callback: ()->Unit){
        this.onCreate = callback
    }

    /*fun setPreviewListener(listener: PreviewCreateListener){
        this.listener = listener
    }

    interface PreviewCreateListener{
        fun onPreviewCreate()
    }*/
}