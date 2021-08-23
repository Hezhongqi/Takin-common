/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shulie.takin.channel.handler;

import io.shulie.takin.channel.bean.CommandPacket;
import io.shulie.takin.channel.bean.CommandResponse;

/**
 * @author: Hengyu
 * @className: CommandHandler
 * @date: 2020/12/29 11:29 下午
 * @description: 定义命令处理
 */
public interface CommandHandler {

    /**
     * 命令处理对象
     * @param packet 命令传输信息对象
     * @return 命令处理响应
     */
    public CommandResponse handle(CommandPacket packet);

}
