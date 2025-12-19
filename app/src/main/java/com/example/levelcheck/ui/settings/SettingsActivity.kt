package com.example.levelcheck.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.levelcheck.databinding.ActivitySettingsBinding
import com.example.levelcheck.service.SensorService
import com.example.levelcheck.ui.calibration.CalibrationActivity
import com.example.levelcheck.util.Constants
import com.example.levelcheck.util.toTimeString

/**
 * 设置界面Activity
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    private var isForceConfigMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        
        // 检查是否为强制配置模式
        isForceConfigMode = intent.getBooleanExtra(Constants.INTENT_EXTRA_FORCE_CONFIG_MODE, false)
        
        setupViews()
        observeViewModel()
    }
    
    /**
     * 设置视图
     */
    private fun setupViews() {
        // SeekBar进度变化监听
        binding.seekBarInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // progress为0-29，实际值为1-30
                val actualValue = progress + 1
                binding.tvIntervalValue.text = actualValue.toString()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 前往校准按钮
        binding.btnGoToCalibration.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            if (isForceConfigMode) {
                showForceConfigCancelDialog()
            } else {
                finish()
            }
        }
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 服务器地址
        viewModel.serverHost.observe(this) { host ->
            binding.etServerHost.setText(host)
        }
        
        // 服务器端口
        viewModel.serverPort.observe(this) { port ->
            binding.etServerPort.setText(port.toString())
        }
        
        // 上传频率
        viewModel.uploadInterval.observe(this) { interval ->
            // interval为1-30，SeekBar进度为0-29
            binding.seekBarInterval.progress = interval - 1
            binding.tvIntervalValue.text = interval.toString()
        }
        
        // 校准时间
        viewModel.calibrationTime.observe(this) { time ->
            binding.tvCalibrationTime.text = time.toTimeString()
        }
        
        // 验证错误
        viewModel.validationError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 刷新校准信息
        viewModel.loadConfig()
    }
    
    /**
     * 保存设置
     */
    private fun saveSettings() {
        val host = binding.etServerHost.text.toString()
        val port = binding.etServerPort.text.toString()
        val interval = binding.seekBarInterval.progress + 1
        
        val success = viewModel.validateAndSaveConfig(host, port, interval)
        
        if (success) {
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            
            // 如果是强制配置模式，标记首次配置完成
            if (isForceConfigMode) {
                viewModel.markFirstLaunchCompleted()
            }
            
            // 通知服务更新配置
            val intent = Intent(this, SensorService::class.java).apply {
                action = Constants.ACTION_UPDATE_CONFIG
            }
            startService(intent)
            
            finish()
        }
    }
    
    /**
     * 显示强制配置模式取消确认对话框
     */
    private fun showForceConfigCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认退出")
            .setMessage("配置尚未完成，退出后应用将无法正常使用。\n\n确定要退出吗？")
            .setPositiveButton("继续配置", null)
            .setNegativeButton("退出应用") { _, _ ->
                // 直接关闭应用
                finishAffinity()
            }
            .show()
    }
    
    override fun onBackPressed() {
        if (isForceConfigMode) {
            showForceConfigCancelDialog()
        } else {
            super.onBackPressed()
        }
    }
}
