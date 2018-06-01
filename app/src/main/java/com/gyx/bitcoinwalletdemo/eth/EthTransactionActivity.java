package com.gyx.bitcoinwalletdemo.eth;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gyx.bitcoinwalletdemo.BaseActivity;
import com.gyx.bitcoinwalletdemo.R;
import com.gyx.bitcoinwalletdemo.Web3JService;
import com.gyx.bitcoinwalletdemo.bean.KeyStoreBean;
import com.gyx.bitcoinwalletdemo.util.KeyStoreUtils;
import com.gyx.bitcoinwalletdemo.zxing.activity.CaptureActivity;
import com.gyx.bitcoinwalletdemo.zxing.utils.CommonUtil;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class EthTransactionActivity extends BaseActivity implements View.OnClickListener {
	private TextView tvBalance;
	private Button btRefresh;
	private TextView tvSendAddress;
	private EditText edReceiveAddress;
	private Button btScanerAddress;
	private EditText edValue;
	private EditText edGasPrice;
	private EditText edGasLimit;
	private EditText edNonce;
	private Button btnSend;
	private TextView tvMgs;
	private Button btnCalculate;
	private TextView tvMiningPrice;
	private Button btTransactionAll;
	//打开扫描界面请求码
	private static final int REQUEST_CODE = 0x01;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_eth_transaction);
		initView();
	}

	private void initView() {
		tvBalance = (TextView) findViewById(R.id.tv_balance);
		btRefresh = (Button) findViewById(R.id.bt_refresh);
		tvSendAddress = (TextView) findViewById(R.id.tv_send_address);
		edReceiveAddress = (EditText) findViewById(R.id.ed_receive_address);
		btScanerAddress = (Button) findViewById(R.id.bt_scaner_address);
		edValue = (EditText) findViewById(R.id.ed_value);
		edGasPrice = (EditText) findViewById(R.id.ed_gas_price);
		edGasLimit = (EditText) findViewById(R.id.ed_gas_limit);
		edNonce = (EditText) findViewById(R.id.ed_nonce);
		btnSend = (Button) findViewById(R.id.btn_send);
		tvMgs = (TextView) findViewById(R.id.tv_mgs);
		btnCalculate = (Button) findViewById(R.id.btn_calculate);
		tvMiningPrice = (TextView) findViewById(R.id.tv_mining_price);
		btTransactionAll = (Button) findViewById(R.id.bt_transaction_all);
		//
		btScanerAddress.setOnClickListener(this);
		btnSend.setOnClickListener(this);
		btnCalculate.setOnClickListener(this);
		btRefresh.setOnClickListener(this);
		btTransactionAll.setOnClickListener(this);
		getNonceFromSendAddress();
		getBalance();
	}

	private void getNonceFromSendAddress() {
		String fromAddress = null;
		List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//		for (KeyStoreBean bean : keyStoreBeans) {
//			fromAddress = bean.getAddress();
//			tvSendAddress.setText("ETH地址:0x" + fromAddress);
//		}
		final String address = keyStoreBeans.get(0).getAddress();
		tvSendAddress.setText("ETH地址:0x" + address);
		Observable.create(new ObservableOnSubscribe<EthGetTransactionCount>() {
			@Override
			public void subscribe(ObservableEmitter<EthGetTransactionCount> e) throws Exception {
				EthGetTransactionCount count = Web3JService.getInstance().ethGetTransactionCount("0x" + address, DefaultBlockParameterName.LATEST).send();
				if (count.getError() == null) {
					e.onNext(count);
					e.onComplete();
				} else {
					e.onError(new Throwable(count.getError().getMessage()));
				}
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<EthGetTransactionCount>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(EthGetTransactionCount ethGetTransactionCount) {
						BigInteger bigInteger = Numeric.decodeQuantity(ethGetTransactionCount.getResult());
						edNonce.setText(bigInteger.toString());
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}
				});
	}

	/**
	 * 获取余额
	 *
	 * @param
	 * @return 余额
	 */
	private void getBalance() {
//		BigInteger balance = null;
//		try {
//			Web3j web3j = Web3JService.getInstance();
//			EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
//			balance = ethGetBalance.getBalance();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//			Log.e("错误", e.getMessage());
//		}
//		Log.e("获取Balance---", "address " + address + " balance " + balance + "wei");
//		BigDecimal bigDecimal = Convert.fromWei(new BigDecimal(balance), Convert.Unit.ETHER);
//
//		return bigDecimal.toString();
		List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//
		KeyStoreBean keyStoreBean = keyStoreBeans.get(0);
		final String address = keyStoreBean.getAddress();
		Observable.create(new ObservableOnSubscribe<EthGetBalance>() {
			@Override
			public void subscribe(ObservableEmitter<EthGetBalance> e) throws Exception {
				Web3j web3j = Web3JService.getInstance();
				EthGetBalance ethGetBalance = web3j.ethGetBalance("0x" + address, DefaultBlockParameterName.LATEST).send();
				e.onNext(ethGetBalance);
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<EthGetBalance>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(EthGetBalance ethGetBalance) {
						if (ethGetBalance != null) {
							if (ethGetBalance.getError() == null) {
								//换算出ETHER
								BigDecimal balanceDecimal = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
								tvBalance.setText(balanceDecimal.toPlainString());
							}
						}
					}

					@Override
					public void onError(Throwable e) {

					}

					@Override
					public void onComplete() {
					}
				});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.bt_scaner_address:
				cameraTask();
				break;
			case R.id.btn_send:
				sendTrasaction();
				break;
			//计算矿工费
			case R.id.btn_calculate:
				String gasPrice = edGasPrice.getText().toString().trim();

				String gasLimit = edGasLimit.getText().toString().trim();
				if (TextUtils.isEmpty(gasPrice) || TextUtils.isEmpty(gasLimit)) {
					Toast.makeText(this, "gasPrice和gasLimit不能为空", Toast.LENGTH_SHORT).show();

				} else {
					BigDecimal miningPrice = getMiningPrice(gasPrice, gasLimit);
					tvMiningPrice.setText("矿工费：" + miningPrice.toString() + "ETH");
				}



				break;
			case R.id.bt_refresh:
				getBalance();
				break;
			//全部转出
			case R.id.bt_transaction_all:
				String gasPrice1 = edGasPrice.getText().toString().trim();

				String gasLimit1 = edGasLimit.getText().toString().trim();
				if (TextUtils.isEmpty(gasPrice1) || TextUtils.isEmpty(gasLimit1)) {
					Toast.makeText(this, "gasPrice和gasLimit不能为空", Toast.LENGTH_SHORT).show();

				} else {
					BigDecimal miningPrice = getMiningPrice(gasPrice1, gasLimit1);
					tvMiningPrice.setText("矿工费：" + miningPrice.toString() + "ETH");
					BigDecimal balance = new BigDecimal(tvBalance.getText().toString().trim());
					//先减
					String transactionValue = balance.subtract(miningPrice).toPlainString();
					edValue.setText(transactionValue);

				}


				break;
		}
	}

	/**
	 * 计算矿工费
	 * @param gasPrice
	 * @param gasLimit
	 * @return
	 */
	private BigDecimal getMiningPrice(String gasPrice, String gasLimit) {


		Double gasPriceDouble = Double.valueOf(gasPrice);
		BigInteger bigGasPrice = Convert.toWei(BigDecimal.valueOf(gasPriceDouble), Convert.Unit.GWEI).toBigInteger();
		BigInteger gasLimitInteger = new BigInteger(gasLimit);
		BigInteger multiply = bigGasPrice.multiply(gasLimitInteger);
		Log.e("乘积", multiply.toString());
		//换算成ether单位
		BigDecimal bigDecimal2 = Convert.fromWei(new BigDecimal(multiply), Convert.Unit.ETHER);
		return bigDecimal2;
	}

	/**
	 * 发送交易
	 */
	private void sendTrasaction() {
//		final String from = tvSendAddress.getText().toString().trim();
		List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//
		KeyStoreBean keyStoreBean = keyStoreBeans.get(0);
		final String from = keyStoreBean.getAddress();//没有0x开头
		//
		final String to = edReceiveAddress.getText().toString().trim();
		//
		final String value = edValue.getText().toString().trim();
		Double aDouble = Double.valueOf(value);
		//toWei转换为18位的wei
		final BigInteger bigValue = Convert.toWei(BigDecimal.valueOf(aDouble), Convert.Unit.ETHER).toBigInteger();
		//gasPrice
		final String gasPrice = edGasPrice.getText().toString().trim();
		Double gasPriceDouble = Double.valueOf(gasPrice);
		final BigInteger bigGasPrice = Convert.toWei(BigDecimal.valueOf(gasPriceDouble), Convert.Unit.GWEI).toBigInteger();
		//gasLimit
		final String gasLimit = edGasLimit.getText().toString().trim();

		final String nonce = edNonce.getText().toString().trim();



		Observable.create(new ObservableOnSubscribe<EthSendTransaction>() {
			@Override
			public void subscribe(ObservableEmitter<EthSendTransaction> e) throws Exception {
				Web3j web3j = Web3JService.getInstance();
				//签名交易信息
				String hexValue = KeyStoreUtils.signedTransactionData(from, to, nonce, bigGasPrice.toString(), gasLimit, bigValue.toString());
				//RPC 处理发送交易
				EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).send();


//				Log.e("transaction1", send.getJsonrpc());
//				Log.e("transaction2", send.getRawResponse());
//				Log.e("transaction3", send.getResult());
				if (send.getError() == null) {
					e.onNext(send);
					e.onComplete();
				} else {
					e.onError(new Throwable(send.getError().getMessage()));
				}
			}
		}).subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<EthSendTransaction>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(EthSendTransaction ethSendTransaction) {
						String result = ethSendTransaction.getResult();
						if (ethSendTransaction.getError() == null) {
							Log.e("transaction", result + "");
							tvMgs.setText(result + "");
							Toast.makeText(EthTransactionActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
						} else {
							tvMgs.setText(ethSendTransaction.getJsonrpc());
							Log.e("transaction1", ethSendTransaction.getJsonrpc());
							Log.e("transaction2", ethSendTransaction.getRawResponse());
							Log.e("transaction3", ethSendTransaction.getResult());
						}
					}

					@Override
					public void onError(Throwable e) {
						e.printStackTrace();
						Toast.makeText(EthTransactionActivity.this, "发送失败", Toast.LENGTH_SHORT).show();

						tvMgs.setText(e.getMessage());
						Log.e("transaction", e.getMessage());
					}

					@Override
					public void onComplete() {
					}
				});
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
			edReceiveAddress.setText(scanResult);
		}
	}
}
