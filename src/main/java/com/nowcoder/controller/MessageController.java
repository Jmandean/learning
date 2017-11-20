package com.nowcoder.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nowcoder.dao.MessageDAO;
import com.nowcoder.model.Comment;
import com.nowcoder.model.EntityType;
import com.nowcoder.model.HostHolder;
import com.nowcoder.model.Message;
import com.nowcoder.model.User;
import com.nowcoder.model.ViewObject;
import com.nowcoder.service.MessageService;
import com.nowcoder.service.UserService;
import com.nowcoder.util.ToutiaoUtil;

@Controller
public class MessageController {
	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

	@Autowired
	MessageDAO MessageDAO;

	@Autowired
	MessageService messageService;

	@Autowired
	UserService userService;

	@Autowired
	HostHolder hostHolder;

	@RequestMapping(path = {"/msg/list"}, method = {RequestMethod.GET})
    public String conversationList(Model model) {
        try {
            int localUserId = hostHolder.getUser().getId();
            List<ViewObject> conversations = new ArrayList<>();
            List<Message> conversationList = messageService.getConversationList(localUserId, 0, 10);
            for (Message msg : conversationList) {
                ViewObject vo = new ViewObject();
                vo.set("conversation", msg);
                int targetId = msg.getFromId() == localUserId ? msg.getToId() : msg.getFromId();
                User user = userService.getUser(targetId);
                vo.set("headUrl", user.getHeadUrl());
                vo.set("userName", user.getName());
                vo.set("targetId", targetId);
                vo.set("totalCount", msg.getId());
                vo.set("unreadCount", messageService.getConvesationUnreadCount(localUserId, msg.getConversationId()));
                conversations.add(vo);
            }
            logger.info(conversations.toString());
            model.addAttribute("conversations", conversations);
            
        } catch (Exception e) {
            logger.error("获取站内信列表失败" + e.getMessage());
        }
        return "letter";
    }

	@RequestMapping(path = { "/msg/detail" }, method = { RequestMethod.GET })
	public String conversationList(Model model, @Param("conversationId") String conversationId) {
		try {
			List<Message> conversationList = messageService.getConversationDetail(conversationId, 0, 10);
			List<ViewObject> messages = new ArrayList<>();
			for (Message msg : conversationList) {
				ViewObject vo = new ViewObject();
				messages.add(vo);
				vo.set("message", msg);
				User user = userService.getUser(msg.getFromId());
				if (user == null) {
					continue;
				}
				vo.set("headUrl", user.getHeadUrl());
				vo.set("userId", user.getId());
				messages.add(vo);

			}
			model.addAttribute("messages", messages);
		} catch (Exception e) {
			logger.error("获取详情消息失败");
		}

		return "letterDetail";
	}

	@RequestMapping(path = { "/msg/addMessage" }, method = { RequestMethod.POST })
	@ResponseBody
	public String addMessage(@RequestParam("fromId") int fromId, @RequestParam("toId") int toId,
			@RequestParam("content") String content) {
		try {
			Message msg = new Message();
			msg.setContent(content);
			msg.setFromId(fromId);
			msg.setToId(toId);
			msg.setCreatedDate(new Date());
			msg.setConversationId(
					fromId < toId ? String.format("%d_%d", fromId, toId) : String.format("%d_%d", toId, fromId));
			messageService.addMessage(msg);
			return ToutiaoUtil.getJSONString(msg.getId());
		} catch (Exception e) {
			logger.error("增加评论失败" + e.getMessage());
			return ToutiaoUtil.getJSONString(1, "插入评论失败");
		}

	}
}
