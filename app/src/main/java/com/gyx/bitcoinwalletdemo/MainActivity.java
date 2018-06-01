package com.gyx.bitcoinwalletdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.gyx.bitcoinwalletdemo.bean.AddressBalanceBean;
import com.gyx.bitcoinwalletdemo.callback.OnDownLoadListener;
import com.gyx.bitcoinwalletdemo.zxing.activity.CaptureActivity;
import com.gyx.bitcoinwalletdemo.zxing.utils.CommonUtil;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity implements View.OnClickListener {
	public String Tag = "BitCoinWalletDemo";
	private TextView tv_balance;
	private TextView address;
	private TextView privtekey;
	private TextView mnemonic;
	private EditText edMnemonic;
	private Button scaner;
	private Button recovery;

	private EditText edTransctionAmount;
	private EditText edTransctionAddress;
	private Button btScanerAddress;
	private TextView tvTransctionCallbackData;
	private Button btSend;
	private NetworkParameters parameters;
	/**
	 * 请求CAMERA权限码
	 */
	public static final int REQUEST_CAMERA_PERM = 101;
	//打开扫描界面请求码
	private static final int REQUEST_CODE = 0x01;
	private static final int ID_BALANCE_LOADER = 0;
	private Wallet wallet;
	public File file;
	private MyHandler myHandler = new MyHandler(this);
	private ProgressDialog progressDialog;
	private WalletAppKit kit;

	private static class MyHandler extends Handler {
		//弱引用
		private WeakReference<Context> reference;

		public MyHandler(Context mContext) {
			reference = new WeakReference<Context>(mContext);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity context = (MainActivity) reference.get();
			context.showWalletInfo(context.file);
			context.progressDialog.dismiss();
//			context.showBalance();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		file = new File(getFilesDir(), "btctest");
		initView();
	}

	private void initView() {
		tv_balance = (TextView) findViewById(R.id.balance);
		address = (TextView) findViewById(R.id.address);
		privtekey = (TextView) findViewById(R.id.privtekey);
		mnemonic = (TextView) findViewById(R.id.mnemonic);
		edMnemonic = (EditText) findViewById(R.id.ed_mnemonic);
		scaner = (Button) findViewById(R.id.scaner);
		recovery = (Button) findViewById(R.id.recovery);


		edTransctionAmount = (EditText) findViewById(R.id.ed_transction_amount);
		edTransctionAddress = (EditText) findViewById(R.id.ed_transction_address);
		btScanerAddress = (Button) findViewById(R.id.bt_scaner_address);
		tvTransctionCallbackData = (TextView) findViewById(R.id.tv_transction_callback_data);
		btSend = (Button) findViewById(R.id.bt_send);

		scaner.setOnClickListener(this);
		recovery.setOnClickListener(this);
		btScanerAddress.setOnClickListener(this);
		btSend.setOnClickListener(this);
		progressDialog = new ProgressDialog(this);
		initWallet();
	}

	private void initWallet() {
		progressDialog.show();
		new Thread() {
			@Override
			public void run() {
				super.run();
				parameters = TestNet3Params.get();
				//先实例化一个file对象，参数为路径名
//				file = new File("/mnt/sdcard/cointest.txt");
				if (!file.exists()) {
					//创建钱包
					wallet = new Wallet(parameters);
//				kit = new WalletAppKit(parameters, new File(getFilesDir().getPath()), "btctest");
//				new PeerGroup(parameters).setFastCatchupTimeSecs(1522389211L);
//				kit.startAsync();
//				kit.awaitRunning();
					myHandler.sendEmptyMessage(0);
				} else {
					WalletExtension walletExtension = new WalletExtension() {
						@Override
						public String getWalletExtensionID() {
							return null;
						}

						@Override
						public boolean isWalletExtensionMandatory() {
							return false;
						}

						@Override
						public byte[] serializeWalletExtension() {
							return new byte[0];
						}

						@Override
						public void deserializeWalletExtension(Wallet containingWallet, byte[] data) throws Exception {
						}
					};
					//从本地恢复钱包
					try {
						wallet = Wallet.loadFromFile(file, walletExtension);
//						showWalletInfo(file);
						myHandler.sendEmptyMessage(0);
					} catch (UnreadableWalletException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public void showWalletInfo(File file) {
		//获取助记词
		DeterministicSeed seed = wallet.getKeyChainSeed();
		//地址
		Address currentReceiveAddress = wallet.currentReceiveAddress();
		String addressString = currentReceiveAddress.toBase58();
		address.setText("地址：" + addressString);
		Log.e(Tag, "地址：" + addressString);
		//密钥
		DeterministicKey deterministicKey = wallet.currentReceiveKey();
		String privateKeyAsWiF = deterministicKey.getPrivateKeyAsWiF(parameters);
		privtekey.setText("私钥：" + privateKeyAsWiF);
		Log.e(Tag, "私钥：" + privateKeyAsWiF);
		//org.bitcoinj.core.Context.propagate(new org.bitcoinj.core.Context(TestNet3Params.get()));
		/*Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);

		long value = balance.getValue();
		tv_balance.setText("金额：" + value);*/
		mnemonic.setText("助记词：" + Joiner.on(" ").join(seed.getMnemonicCode()));
		Log.e(Tag, "助记词：" + Joiner.on(" ").join(seed.getMnemonicCode()));
		//金额的loader
		//getSupportLoaderManager().initLoader(ID_BALANCE_LOADER, null, balanceLoaderCallbacks);
		//Coin addressBalance = wallet.getBalance(new AddressBalance(currentReceiveAddress));
		//Log.e("地址金额------", addressBalance.getValue() + "");
		RequestUtils.getInstance().getAddressBalance(addressString, new OnDownLoadListener<AddressBalanceBean>() {
			@Override
			public void onSuccess(AddressBalanceBean addressBalanceBean) {
				String final_balance = addressBalanceBean.getFinal_balance();

			}

			@Override
			public void onFailed(Exception e) {
			}
		});
		RequestUtils.getInstance().getUnspent(addressString, new OnDownLoadListener<String>() {
			@Override
			public void onSuccess(String addressBalanceBean) {
			}

			@Override
			public void onFailed(Exception e) {
			}
		});
		try {
			wallet.saveToFile(file);
//			wallet.autosaveToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.scaner:
				cameraTask();
				break;
			case R.id.recovery:
				String trim = edMnemonic.getText().toString().trim();
				if (TextUtils.isEmpty(trim)) {
					Toast.makeText(this, "助记词不能为空", Toast.LENGTH_SHORT).show();
				} else {
					//恢复
					recoveryWallet(trim);
				}


				break;

			//扫描地址
			case R.id.bt_scaner_address:
				break;
			//发送
			case R.id.bt_send:
				break;
		}
	}

	/**
	 * 恢复钱包
	 *
	 * @param seedCode
	 */
	private void recoveryWallet(final String seedCode) {
		progressDialog.show();
		new Thread() {
			@Override
			public void run() {
				super.run();
				//String seedCode = "horror zone smile brown pig aunt text onion lawsuit glad twelve office";
				long creationtime = 1522389211L;
				DeterministicSeed seed = null;
				try {
					parameters = TestNet3Params.get();
					seed = new DeterministicSeed(seedCode, null, "", creationtime);
					wallet = Wallet.fromSeed(parameters, seed);
					myHandler.sendEmptyMessage(0);
				} catch (UnreadableWalletException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}


	///////////////////////////////////////////////////////////////////////////
	//摄像头扫码部分
	///////////////////////////////////////////////////////////////////////////

	@AfterPermissionGranted(REQUEST_CAMERA_PERM)
	public void cameraTask() {
		if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
			// Have permission, do the thing!
			onViewClick();
		} else {
			// Ask for one permission
			EasyPermissions.requestPermissions(this, "需要请求camera权限", REQUEST_CAMERA_PERM, Manifest.permission.CAMERA);
		}
	}

	/**
	 * 打开摄像头
	 */
	private void onViewClick() {
		if (CommonUtil.isCameraCanUse()) {
			Intent intent = new Intent(this, CaptureActivity.class);
			startActivityForResult(intent, REQUEST_CODE);
		} else {
			Toast.makeText(this, "打开摄像头权限", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//扫描完的结果，之后就是恢复钱包
		//扫描结果回调
		if (resultCode == RESULT_OK) { //RESULT_OK = -1
			Bundle bundle = data.getExtras();
			String scanResult = bundle.getString("qr_scan_result");
			//将扫描出的信息显示出来
			//Toast.makeText(this, scanResult, Toast.LENGTH_SHORT).show();
			edMnemonic.setText(scanResult);
		}
	}
}
















