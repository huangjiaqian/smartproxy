package org.huangjiaqqian.smartproxy.server.helper;

import java.math.BigDecimal;

import cn.hutool.core.convert.Convert;

public class Helper {
	
	public static final String displayFlow(Object upFlow, Object downFlow) {
		StringBuilder sb = new StringBuilder();
		

		sb.append("上传：").append(displayFlow(upFlow));
		
		sb.append("&nbsp;&nbsp;");

		sb.append("下载:").append(displayFlow(downFlow));
		return sb.toString();
	}
	
	public static final String displayFlow(Object flow) {
		Double flowD = Convert.toDouble(flow, 0D);
		BigDecimal decimal = new BigDecimal(flowD);
		final BigDecimal comp = new BigDecimal(1024D); 
		
		String dw = " B";

		if(decimal.compareTo(comp) == 1) {
			dw = " K";
			decimal = decimal.divide(new BigDecimal(1024));
		}
		
		if(decimal.compareTo(comp) == 1) {
			dw = " M";
			decimal = decimal.divide(new BigDecimal(1024));
		}
		
		if(decimal.compareTo(comp) == 1) {
			dw = " G";
			decimal = decimal.divide(new BigDecimal(1024));
		}
		
		if(decimal.compareTo(comp) == 1) {
			dw = " T";
			decimal = decimal.divide(new BigDecimal(1024));
		}
		
		decimal = decimal.setScale(2, BigDecimal.ROUND_HALF_UP);
		return decimal.toString() + dw;
	}
	public static void main(String[] args) {
		BigDecimal decimal = new BigDecimal(1025D);
		
		System.out.println(decimal.compareTo(new BigDecimal(1026)));
		
	}
}
