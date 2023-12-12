/*
 *  Copyright (C) 2023 Consiglio Nazionale delle Ricerche
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.cnr.anac.transparency.rules.service;

import it.cnr.anac.transparency.rules.configuration.RuleConfiguration;
import it.cnr.anac.transparency.rules.domain.Anchor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Slf4j
@Service
public class SeleniumAnchorService {
    WebDriver driver;
    @Autowired
    AnchorService anchorService;
    @Autowired
    RuleConfiguration ruleConfiguration;
    @Autowired
    AbstractDriverOptions abstractDriverOptions;

    @Synchronized
    public List<Anchor> findAnchor(String url) {
        driverGet(url);
        final String pageSource = driver.getPageSource();
        log.trace("===============BEGING PAGE SOURCE==============");
        log.trace(pageSource);
        log.trace("===============END PAGE SOURCE==============");
        return anchorService.find(pageSource);
    }

    private void driverGet(String url) {
        try {
            if (driver == null) {
                try {
                    log.debug("Try to start driver.....");
                    driver = new RemoteWebDriver(new URL(ruleConfiguration.getSelenium_url()), abstractDriverOptions);
                    driver.manage().window().maximize();
                    log.debug("Driver is started");
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            log.debug("Start to get url: {}", url);
            driver.get(url);
            log.debug("Finish to get url: {}", url);
        } catch (NoSuchSessionException _ex) {
            driver = null;
            driverGet(url);
        }
    }
}
