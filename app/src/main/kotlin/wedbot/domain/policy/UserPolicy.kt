package wedbot.domain.policy

import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo

fun UserInfo.canViewAdminPanel(): Boolean {
    return role == Role.ADMIN
}

fun UserInfo.canDownloadCalendar(): Boolean {
    return role == Role.GUEST && eventStatus == Status.APPROVED
}

fun UserInfo.canViewLocation(): Boolean {
    return role == Role.GUEST && eventStatus == Status.APPROVED
}

fun UserInfo.canChangeStatus(): Boolean {
    return role == Role.GUEST && eventStatus == Status.THINKING
}

fun UserInfo.canViewInfo(): Boolean {
    return eventStatus != Status.SLEEVE
}

fun UserInfo.canContactOrganizers(): Boolean {
    return role != Role.ADMIN
}
