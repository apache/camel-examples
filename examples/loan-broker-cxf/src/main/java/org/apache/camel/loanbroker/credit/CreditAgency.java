/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.loanbroker.credit;

import java.util.Random;

//START SNIPPET: creditAgencyImpl
public class CreditAgency implements CreditAgencyWS {

    private final Random random = new Random();

    @Override
    public int getCreditHistoryLength(String ssn) {
        return random.nextInt() * 600 + 300;
    }

    @Override
    public int getCreditScore(String ssn) {
        return random.nextInt() * 19 + 1;
    }

}
//END SNIPPET: creditAgencyImpl
