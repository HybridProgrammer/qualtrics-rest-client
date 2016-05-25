package edu.fau.qualtrics.domain

/**
 * Copyright 2013 Jason Heithoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * User: jason
 * Date: 5/18/16
 * Time: 6:41 AM
 *
 */
class LoginActivity {
    int ever
    int never
    int past120Days
    int past14Days
    int past1Days
    int past30Days
    int past60Days
    int past7Days
    int past90Days
    int totalUsers

    public Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [ (it.name):this."$it.name" ]
        }
    }

    LoginActivity(Map map) {
        hydrateData(map)
    }

    private void hydrateData(Map map) {
        metaClass.setProperties(this, map.findAll { key, value ->
            this.hasProperty(key)
        })
    }

    @Override
    public String toString() {
        return "LoginActivity{" +
                "ever=" + ever +
                ", never=" + never +
                ", past120Days=" + past120Days +
                ", past14Days=" + past14Days +
                ", past1Days=" + past1Days +
                ", past30Days=" + past30Days +
                ", past60Days=" + past60Days +
                ", past7Days=" + past7Days +
                ", past90Days=" + past90Days +
                ", totalUsers=" + totalUsers +
                '}';
    }
}
