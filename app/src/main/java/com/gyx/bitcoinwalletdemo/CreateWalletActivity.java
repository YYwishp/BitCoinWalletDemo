package com.gyx.bitcoinwalletdemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.gyx.bitcoinwalletdemo.btcCoin.BtcTransactionActivity;
import com.gyx.bitcoinwalletdemo.btcCoin.azazar.bitcoin.jsonrpcclient.Bitcoin;
import com.gyx.bitcoinwalletdemo.btcCoin.azazar.bitcoin.jsonrpcclient.BitcoinException;
import com.gyx.bitcoinwalletdemo.btcCoin.azazar.bitcoin.jsonrpcclient.BitcoinJSONRPCClient;
import com.gyx.bitcoinwalletdemo.eth.EthContractTransactionActivity;
import com.gyx.bitcoinwalletdemo.eth.EthTransactionActivity;
import com.gyx.bitcoinwalletdemo.eth.EthWalletUtil;
import com.gyx.bitcoinwalletdemo.util.KeyStoreUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;
import org.web3j.crypto.ECKeyPair;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static rx.schedulers.Schedulers.test;

public class CreateWalletActivity extends AppCompatActivity {
	private TextView tvEthAddress;
	private TextView tvEthPrivatekey;
	private TextView tvEthBalance;
	private TextView tvBtcAddress;
	private TextView tvBtcPrivatekey;
	private TextView tvBtcBalance;
	private TextView tvMnemonic;
	private NetworkParameters parameters;
	private Wallet wallet;
	private String Tag = "创建";
	private File fireBtc;
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_wallet);
		tvMnemonic = (TextView) findViewById(R.id.tv_mnemonic);
		tvEthAddress = (TextView) findViewById(R.id.tv_eth_address);
		tvEthPrivatekey = (TextView) findViewById(R.id.tv_eth_privatekey);
		tvEthBalance = (TextView) findViewById(R.id.tv_eth_balance);
		//
		tvBtcAddress = (TextView) findViewById(R.id.tv_btc_address);
		tvBtcPrivatekey = (TextView) findViewById(R.id.tv_btc_privatekey);
		tvBtcBalance = (TextView) findViewById(R.id.tv_btc_balance);
		progressDialog = new ProgressDialog(this);
		createBtcEthWallet();
	}

	/**
	 * 测试比特币RPC接口
	 *
	 */
	private void test8() {
		BitcoinJSONRPCClient bitcoin = new BitcoinJSONRPCClient();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Bitcoin.Info info = bitcoin.getInfo();
					final String s = info.toString();
					//List<Bitcoin.Unspent> unspents = bitcoin.listUnspent();

					List<Bitcoin.Unspent> unspents = bitcoin.listUnspent(1, 99999);
					String hex = "010000000001014be5393e4ee79bde40558f637679ab04a9a90a02409a927d3a542e46537e5e05000000006a473044022034b35843bac9bac485724b042ca31f6879e3766a3540402a437bf5bb5fd7e4ea02205149a8e1c3704f80814b8c6d69d646221cab6c8eab42fb14e3279ab1a7424427012102a2af8bbb39a0b66eb3b26436133a8becbde5dad297f02c238280ae78721f0210ffffffff02002d3101000000001976a914f65af6aaeb109376da8cbbcd4ed6c4bc9a0ff1bf88ac7840ae02000000001976a91467cfec849adc3fa8f3a4c692f39b79e9a4b6229f88ac0000000000";
					// final String s1 = bitcoin.sendRawTransaction(hex);//发送交易签名
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvBtcBalance.setText(s);
						}
					});
				} catch (BitcoinException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * 创建比特币钱包
	 */
	private void createBtcEthWallet() {
		progressDialog.show();
		Observable.create(new ObservableOnSubscribe<Wallet>() {
			@Override
			public void subscribe(ObservableEmitter<Wallet> e) throws Exception {
				//初始化BTC钱包
				Wallet wallet = initBtcWallet();
				if (wallet != null) {
					DeterministicSeed seed = wallet.getKeyChainSeed();
					List<String> mnemonicCode = seed.getMnemonicCode();
					if (mnemonicCode.size() == 12) {
						//创建以太坊钱包
						createEthWallet(mnemonicCode);
					}
					e.onNext(wallet);
					e.onComplete();
				}
			}
		}).subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<Wallet>() {
					@Override
					public void accept(Wallet wallet) throws Exception {
						if (wallet != null) {
							//保存btc文件
							try {
								wallet.saveToFile(fireBtc);
//								wallet.autosaveToFile();
							} catch (IOException e) {
								e.printStackTrace();
							}
							Log.e("线程2", Thread.currentThread().getName());
							//获取助记词
							DeterministicSeed seed = wallet.getKeyChainSeed();
							List<String> mnemonicCode = seed.getMnemonicCode();
							String mnemonic_str = Joiner.on(" ").join(mnemonicCode);
							Log.e(Tag, "助记词：" + Joiner.on(" ").join(seed.getMnemonicCode()));
							tvMnemonic.setText("助记词：" + mnemonic_str);
							//密钥
							DeterministicKey deterministicKey = wallet.currentReceiveKey();
							String privateKeyAsWiF = deterministicKey.getPrivateKeyAsWiF(parameters);
							tvBtcPrivatekey.setText("BTC私钥：" + privateKeyAsWiF);
							Log.e(Tag, "BTC私钥：" + privateKeyAsWiF);
							//地址
							Address currentReceiveAddress = wallet.currentReceiveAddress();
							String addressString = currentReceiveAddress.toBase58();
							tvBtcAddress.setText("BTC地址：" + addressString);
							Log.e(Tag, "BTC地址：" + addressString);
						}
					}
				});
//
	}

	/**
	 * 创建以太坊钱包
	 *
	 * @param mnemonic_str 12个mnemonic助记词
	 */
	private void createEthWallet(final List<String> mnemonic_str) {
//
		File[] keyStoreFiles = KeyStoreUtils.getKeyStorePathFile().listFiles();
		if (keyStoreFiles.length > 0) {
			final File keyStoreFile = keyStoreFiles[0];
			final ECKeyPair ecKeyPair = KeyStoreUtils.decryptWallet(keyStoreFile, KeyStoreUtils.DEFAULTKEY);
			//
			/*byte[] checkMnemonicSeedBytes = Numeric.hexStringToByteArray(ecKeyPair.getPrivateKey().toString(16));
			Log.e("ETH验证助记词种子 ", Arrays.toString(checkMnemonicSeedBytes));
			List<String> checkMnemonic = null;
			try {
				checkMnemonic = MnemonicCode.INSTANCE.toMnemonic(checkMnemonicSeedBytes);
			} catch (MnemonicException.MnemonicLengthException e) {
				e.printStackTrace();
			}*/
//			Log.e("ETH验证助记词 ", Arrays.toString(checkMnemonic.toArray()));
			Log.e("从文件读取---ETH私钥：", ecKeyPair.getPrivateKey().toString(16));
			Log.e("从文件读取---ETH地址：", KeyStoreUtils.getAddress(keyStoreFile));
//
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
//					Log.e("线程3", Thread.currentThread().getName());
					tvEthPrivatekey.setText("ETH私钥：" + ecKeyPair.getPrivateKey().toString(16));
					tvEthAddress.setText("ETH地址：" + KeyStoreUtils.getAddress(keyStoreFile));
					progressDialog.dismiss();
				}
			});
			return;
		}
		Observable.create(new ObservableOnSubscribe<EthWalletUtil.EthHDWallet>() {
			@Override
			public void subscribe(ObservableEmitter<EthWalletUtil.EthHDWallet> e) throws Exception {
				EthWalletUtil.EthHDWallet ethHDWallet = EthWalletUtil.importMnemonic(EthWalletUtil.ETH_TYPE, mnemonic_str, "");
//				List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//				for (KeyStoreBean bean : keyStoreBeans) {
//					String eth_address = bean.getAddress();
//					tvEthAddress.setText("ETH地址："+eth_address);
//				}
				if (ethHDWallet != null) {
					e.onNext(ethHDWallet);
					e.onComplete();
				}
			}
		}).subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<EthWalletUtil.EthHDWallet>() {
					@Override
					public void accept(EthWalletUtil.EthHDWallet wallet) throws Exception {
						if (wallet != null) {
							String eth_address = wallet.getAddress();
							tvEthAddress.setText("ETH地址：" + eth_address);
							String privateKey = wallet.getPrivateKey();
							tvEthPrivatekey.setText("ETH私钥：" + privateKey);
							Log.e("ETH私钥：", privateKey);
							progressDialog.dismiss();
						}
					}
				});
	}

	/**
	 * 以太坊交易
	 *
	 * @param view
	 */
	public void ethTransaction(View view) {
		Intent intent = new Intent(this, EthTransactionActivity.class);
		startActivity(intent);
	}

	/**
	 * 以太坊合约交易
	 *
	 * @param view
	 */
	public void ethContractTransaction(View view) {
		startActivity(new Intent(this, EthContractTransactionActivity.class));
	}

	/**
	 * 比特币交易
	 *
	 * @param view
	 */
	public void btcTransaction(View view) {

		startActivity(new Intent(this,BtcTransactionActivity.class));




	}

	private Wallet initBtcWallet() {
		fireBtc = new File(getFilesDir(), "btctest");
		parameters = TestNet3Params.get();
		Wallet wallet = null;
		if (!fireBtc.exists()) {
			//创建钱包
			wallet = new Wallet(parameters);
			Log.e(Tag, "创建新的BTC");
		} else {
			//从本地恢复钱包
			try {
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

				wallet = Wallet.loadFromFile(fireBtc, walletExtension);
				Log.e(Tag, "从文件恢复BTC");
			} catch (UnreadableWalletException e) {
				e.printStackTrace();
			}
		}
		return wallet;
	}
}
