package org.bitcoin.core;


import java.io.Serializable;

/**
 * Classes implementing this interface represent a monetary value, such as a Bitcoin or fiat amount.
 *
 * 表示货币价值，比如比特币或法定货币。该接口包含以下方法：
 */
public interface Monetary extends Serializable {

    /**
     * Returns the absolute value of exponent of the value of a "smallest unit" in scientific notation. For Bitcoin, a
     * satoshi is worth 1E-8 so this would be 8.
     *
     * 返回科学计数法中“最小单位”值的指数的绝对值。对于比特币，1个satoshi的价值是1E-8，因此返回值为8。
     */
    int smallestUnitExponent();

    /**
     * Returns the number of "smallest units" of this monetary value. For Bitcoin, this would be the number of satoshis.
     *
     * 返回该货币价值的“最小单位”数量。对于比特币，这将是satoshi的数量。
     */
    long getValue();

    int signum();
}