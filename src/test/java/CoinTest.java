/*
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.bitcoin.core.Coin;
import org.bitcoin.core.Fiat;
import org.junit.Test;

import java.math.BigDecimal;

import static org.bitcoin.core.Fiat.parseFiat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CoinTest {
    @Test
    public void test(){
        assertEquals(Coin.SATOSHI, Coin.valueOf(1));

        //1个比特币，1亿satoshi
        //2100万个比特币，则2100万亿satoshi
        assertEquals(100000000L, Coin.btcToSatoshi(new BigDecimal(1)));

    }
}
