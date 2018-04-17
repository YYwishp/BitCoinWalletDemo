package com.gyx.bitcoinwalletdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

public class TestActivity extends AppCompatActivity {

	private Button btCreate;
	private Button btRecovery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);



		btCreate = (Button) findViewById(R.id.bt_create);
		btRecovery = (Button) findViewById(R.id.bt_recovery);
		btCreate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});


		btRecovery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				startActivity(new Intent(TestActivity.this,MainActivity.class));

			}
		});

	}
}
