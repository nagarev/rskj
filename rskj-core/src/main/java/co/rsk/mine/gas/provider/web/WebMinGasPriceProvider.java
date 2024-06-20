/*
 * This file is part of RskJ
 * Copyright (C) 2024 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.mine.gas.provider.web;

import co.rsk.config.mining.StableMinGasPriceSystemConfig;
import co.rsk.config.mining.WebStableMinGasSystemConfig;
import co.rsk.mine.gas.provider.MinGasPriceProvider;
import co.rsk.mine.gas.provider.MinGasPriceProviderType;
import co.rsk.mine.gas.provider.StableMinGasPriceProvider;
import co.rsk.net.http.SimpleHttpClient;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class WebMinGasPriceProvider extends StableMinGasPriceProvider {
    private static final Logger logger = LoggerFactory.getLogger("GasPriceProvider");
    private final String url;
    private final JsonPointer jsonPath;
    private final int timeout;
    private final SimpleHttpClient httpClient;
    private final ObjectMapper objectMapper;


    public WebMinGasPriceProvider(StableMinGasPriceSystemConfig config, MinGasPriceProvider fallBackProvider) {
        super(fallBackProvider, config.getMinStableGasPrice(), config.getRefreshRate());
        WebStableMinGasSystemConfig webConfig = config.getWebConfig();
        url = webConfig.getUrl();
        jsonPath = JsonPointer.valueOf(webConfig.getRequestPath());
        timeout = webConfig.getTimeout();
        httpClient = new SimpleHttpClient(webConfig.getTimeout());
        objectMapper = new ObjectMapper();
    }

    public WebMinGasPriceProvider(StableMinGasPriceSystemConfig config, MinGasPriceProvider fallBackProvider, SimpleHttpClient httpClient) {
        super(fallBackProvider, config.getMinStableGasPrice(), config.getRefreshRate());
        WebStableMinGasSystemConfig webConfig = config.getWebConfig();
        url = webConfig.getUrl();
        jsonPath = JsonPointer.valueOf(webConfig.getRequestPath());
        timeout = webConfig.getTimeout();
        this.httpClient = httpClient;
        objectMapper = new ObjectMapper();
    }

    @Override
    protected Optional<Long> getBtcExchangeRate() {
        String response = getResponseFromWeb();
        if (!StringUtils.isBlank(response)) {
            Long price = parsePrice(response);
            return Optional.ofNullable(price);
        }
        logger.error("Error getting min gas price from web provider, empty response.");
        return Optional.empty();
    }

    private Long parsePrice(String response) {
        try {
            JsonNode jObject = objectMapper.readTree(response);
            if (jObject.at(jsonPath).isMissingNode()) {
                return null;
            }
            return objectMapper.readTree(response).at(jsonPath).asLong();
        } catch (Exception e) {
            logger.error("Error parsing min gas price from web provider: {}", e.getMessage());
            return null;
        }
    }

    private String getResponseFromWeb() {
        try {
            String response = httpClient.doGet(url);
            logger.debug("Response from web: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error getting min gas price from web provider: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public MinGasPriceProviderType getType() {
        return MinGasPriceProviderType.WEB;
    }

    public String getUrl() {
        return url;
    }

    public int getTimeout() {
        return timeout;
    }

}

