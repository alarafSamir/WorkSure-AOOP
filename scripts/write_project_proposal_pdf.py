#!/usr/bin/env python3
"""AOOP Lab project proposal PDF — target 1–2 A4 pages."""

from reportlab.lib.colors import HexColor, black, white
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch, mm
from reportlab.platypus import (
    HRFlowable,
    KeepTogether,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

NAVY = HexColor("#1B365D")
TEAL = HexColor("#0F5C5C")
LINE = HexColor("#C5CDD6")
ROW = HexColor("#F4F7FA")
MUTED = HexColor("#334155")


def styles():
    base = getSampleStyleSheet()
    s = {
        "uni": ParagraphStyle(
            "uni",
            parent=base["Normal"],
            fontName="Times-Bold",
            fontSize=13,
            leading=16,
            alignment=TA_CENTER,
            textColor=NAVY,
        ),
        "dept": ParagraphStyle(
            "dept",
            parent=base["Normal"],
            fontName="Times-Italic",
            fontSize=10,
            leading=13,
            alignment=TA_CENTER,
            textColor=MUTED,
        ),
        "doc": ParagraphStyle(
            "doc",
            parent=base["Normal"],
            fontName="Times-Bold",
            fontSize=11,
            leading=14,
            alignment=TA_CENTER,
            textColor=TEAL,
            spaceBefore=4,
            spaceAfter=2,
        ),
        "title": ParagraphStyle(
            "title",
            parent=base["Normal"],
            fontName="Times-Bold",
            fontSize=14,
            leading=18,
            alignment=TA_CENTER,
            textColor=NAVY,
            spaceBefore=6,
            spaceAfter=8,
        ),
        "h": ParagraphStyle(
            "h",
            parent=base["Normal"],
            fontName="Times-Bold",
            fontSize=11,
            leading=14,
            textColor=NAVY,
            spaceBefore=8,
            spaceAfter=3,
        ),
        "body": ParagraphStyle(
            "body",
            parent=base["Normal"],
            fontName="Times-Roman",
            fontSize=10,
            leading=13.2,
            alignment=TA_JUSTIFY,
            textColor=black,
            spaceAfter=4,
        ),
        "bullet": ParagraphStyle(
            "bullet",
            parent=base["Normal"],
            fontName="Times-Roman",
            fontSize=10,
            leading=13,
            leftIndent=14,
            bulletIndent=2,
            textColor=black,
            spaceAfter=1.5,
        ),
        "meta": ParagraphStyle(
            "meta",
            parent=base["Normal"],
            fontName="Times-Roman",
            fontSize=10,
            leading=13,
            alignment=TA_LEFT,
            textColor=black,
        ),
        "cell": ParagraphStyle(
            "cell",
            parent=base["Normal"],
            fontName="Times-Roman",
            fontSize=9.5,
            leading=12,
            textColor=black,
        ),
        "cellb": ParagraphStyle(
            "cellb",
            parent=base["Normal"],
            fontName="Times-Bold",
            fontSize=9.5,
            leading=12,
            textColor=NAVY,
        ),
        "foot": ParagraphStyle(
            "foot",
            parent=base["Normal"],
            fontName="Times-Italic",
            fontSize=8.5,
            leading=11,
            alignment=TA_CENTER,
            textColor=MUTED,
        ),
    }
    return s


def bullet(s, text):
    return Paragraph("•  " + text, s["bullet"])


def build():
    path = "/home/samir/Documents/worksure/WorkSure_AOOP_Project_Proposal.pdf"
    doc = SimpleDocTemplate(
        path,
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="Project Proposal: WorkSure — Advanced Object Oriented Programming Language Lab",
        author="Al Araf Mahmud Samir, Marfatul Hossain, Taofik Ahamed",
    )
    s = styles()
    story = []

    story.append(Paragraph("United International University", s["uni"]))
    story.append(Paragraph("Department of Computer Science and Engineering", s["dept"]))
    story.append(Paragraph("PROJECT PROPOSAL", s["doc"]))
    story.append(
        Paragraph(
            "WorkSure: A Service Marketplace Platform<br/>for Connecting Customers with Verified Workers",
            s["title"],
        )
    )
    story.append(HRFlowable(width="100%", thickness=1.25, color=NAVY, spaceAfter=8))

    course = [
        [
            Paragraph("<b>Course</b>", s["cellb"]),
            Paragraph("Advanced Object Oriented Programming Language Lab", s["cell"]),
            Paragraph("<b>Section</b>", s["cellb"]),
            Paragraph("A", s["cell"]),
        ],
        [
            Paragraph("<b>Course Type</b>", s["cellb"]),
            Paragraph("Laboratory / Group Project", s["cell"]),
            Paragraph("<b>Term</b>", s["cellb"]),
            Paragraph("Academic Lab Project", s["cell"]),
        ],
    ]
    t0 = Table(course, colWidths=[28 * mm, 82 * mm, 28 * mm, 42 * mm])
    t0.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), ROW),
                ("BOX", (0, 0), (-1, -1), 0.4, LINE),
                ("INNERGRID", (0, 0), (-1, -1), 0.3, LINE),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    story.append(t0)
    story.append(Spacer(1, 8))

    story.append(Paragraph("Group Members", s["h"]))
    members = [
        [
            Paragraph("<b>SL</b>", s["cellb"]),
            Paragraph("<b>Name</b>", s["cellb"]),
            Paragraph("<b>Student ID</b>", s["cellb"]),
            Paragraph("<b>Role in the Project</b>", s["cellb"]),
        ],
        [
            Paragraph("1", s["cell"]),
            Paragraph("Al Araf Mahmud Samir", s["cell"]),
            Paragraph("0112230624", s["cell"]),
            Paragraph("Backend (Java/Spring), database, security", s["cell"]),
        ],
        [
            Paragraph("2", s["cell"]),
            Paragraph("Marfatul Hossain", s["cell"]),
            Paragraph("0112230701", s["cell"]),
            Paragraph("Frontend (HTML/CSS/JavaScript), UI/UX, API wiring", s["cell"]),
        ],
        [
            Paragraph("3", s["cell"]),
            Paragraph("Taofik Ahamed", s["cell"]),
            Paragraph("0112230831", s["cell"]),
            Paragraph("Bookings, reviews, testing &amp; documentation", s["cell"]),
        ],
    ]
    t1 = Table(members, colWidths=[12 * mm, 58 * mm, 38 * mm, 72 * mm])
    t1.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), NAVY),
                ("TEXTCOLOR", (0, 0), (-1, 0), white),
                ("BACKGROUND", (0, 1), (-1, 1), white),
                ("BACKGROUND", (0, 2), (-1, 2), ROW),
                ("BACKGROUND", (0, 3), (-1, 3), white),
                ("BOX", (0, 0), (-1, -1), 0.5, NAVY),
                ("INNERGRID", (0, 0), (-1, -1), 0.3, LINE),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    # header cells already have bold navy text in paragraphs - need white for header
    members[0] = [
        Paragraph("<font color='white'><b>SL</b></font>", s["cell"]),
        Paragraph("<font color='white'><b>Name</b></font>", s["cell"]),
        Paragraph("<font color='white'><b>Student ID</b></font>", s["cell"]),
        Paragraph("<font color='white'><b>Role in the Project</b></font>", s["cell"]),
    ]
    t1 = Table(members, colWidths=[12 * mm, 58 * mm, 38 * mm, 72 * mm])
    t1.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), NAVY),
                ("BACKGROUND", (0, 1), (-1, 1), white),
                ("BACKGROUND", (0, 2), (-1, 2), ROW),
                ("BACKGROUND", (0, 3), (-1, 3), white),
                ("BOX", (0, 0), (-1, -1), 0.5, NAVY),
                ("INNERGRID", (0, 0), (-1, -1), 0.3, LINE),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 4.5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4.5),
            ]
        )
    )
    story.append(t1)

    story.append(Paragraph("1. Introduction and Problem Statement", s["h"]))
    story.append(
        Paragraph(
            "Finding a trusted electrician, cleaner, babysitter, or similar worker is still difficult for many people. "
            "Customers do not know who is verified, workers do not have a simple way to receive bookings, and there is "
            "often no clear record of price or job status. For our Advanced Object Oriented Programming Language Lab, "
            "we propose <b>WorkSure</b>, a web-based service marketplace that connects three types of users—customers, workers, "
            "and an admin—through one platform. The project is designed so that object-oriented ideas (classes, encapsulation, "
            "abstraction, layered design, and dependency injection) are applied in a real Java backend, not only in theory.",
            s["body"],
        )
    )

    story.append(Paragraph("2. Objectives", s["h"]))
    story.append(bullet(s, "Build a complete lab project where Java classes work together as a server (API) and a plain HTML/CSS/JavaScript website acts as the user interface."))
    story.append(bullet(s, "Allow customers to browse six service sectors (Cleaning, Electrician, Security, Catering, Babysitting, Pet Care), book a worker and review completed jobs."))
    story.append(bullet(s, "Allow workers to create a profile, list services, accept or reject jobs, upload documents for verification, and read received reviews."))
    story.append(bullet(s, "Give an admin dashboard to verify workers, manage users, and view bookings and reviews."))
    story.append(bullet(s, "Practice OOP in Spring Boot: separate controllers, security, data access, and reusable helper classes, with JWT login and role-based access."))

    story.append(Paragraph("3. Proposed System (What We Will Build)", s["h"]))
    story.append(
        Paragraph(
            "WorkSure will be a three-part system. The <b>frontend</b> is a plain HTML/CSS/JavaScript website (pages, forms, dashboards). "
            "The <b>backend</b> is a Java Spring Boot application that receives requests, checks who is logged in, and applies business rules. "
            "The <b>database</b> is MariaDB/MySQL, which stores users, services, bookings, reviews, and verification records. "
            "The website never talks to the database directly; every click that needs data goes through the Java server. "
            "Spring Boot will serve the HTML pages and API together at http://localhost:5000.",
            s["body"],
        )
    )
    story.append(
        Paragraph(
            "<b>Main modules:</b> (1) Authentication and roles, (2) Service catalog and worker listings, "
            "(3) Customer bookings and worker job transitions, (4) Worker profile and services, "
            "(5) Completed-booking reviews, (6) Private document verification, (7) Admin users and read-only reports.",
            s["body"],
        )
    )

    story.append(Paragraph("4. Technology Stack and Why It Fits This Course", s["h"]))
    story.append(
        Paragraph(
            "<b>Java 17 and Spring Boot</b> are chosen because this is an OOP lab: we can show classes, constructor injection, "
            "interfaces (for example password encoding), a security filter, and exception handling in Java. "
            "<b>Spring JDBC</b> (not JPA) will talk to <b>MariaDB</b> using clear SQL so table design and foreign keys stay visible for learning. "
            "<b>JWT + Spring Security + BCrypt</b> will protect routes (customer / worker / admin). "
            "<b>Plain HTML, CSS and JavaScript with browser fetch()</b> will provide the interface so the Java API can be demonstrated as a full working product. "
            "Together, the stack matches a modern OOP lab: object design on the server, plus a complete end-to-end application.",
            s["body"],
        )
    )

    story.append(Paragraph("5. Object-Oriented Concepts to Be Applied", s["h"]))
    story.append(bullet(s, "<b>Encapsulation:</b> hide secrets and messy details inside classes (JWT signing, file upload rules, password hashing)."))
    story.append(bullet(s, "<b>Abstraction:</b> controllers call simple methods such as query/insert; they do not deal with raw JDBC code."))
    story.append(bullet(s, "<b>Composition and DI:</b> a booking controller <i>has</i> a database helper and a notification service (Spring injects them)."))
    story.append(bullet(s, "<b>Inheritance / template method:</b> a JWT filter extends Spring’s request filter and fills in authentication logic."))
    story.append(bullet(s, "<b>Layered design:</b> HTTP controllers, reusable services (security, commission, uploads), and a data-access class in front of the database."))
    story.append(bullet(s, "<b>Polymorphism:</b> Spring Security and exception handlers treat different error/filter types through common contracts."))

    story.append(Paragraph("6. Expected Outcome and Lab Contribution", s["h"]))
    story.append(
        Paragraph(
            "At the end of the lab, the group will demonstrate a running system: register/login, book a service, change job status, "
            "submit a review, and demonstrate private verification and admin control. The code will be used to explain OOP in a real project—how objects share work, "
            "how roles restrict actions, and how data is saved safely. This proposal asks for faculty approval to complete WorkSure "
            "as our section A group project for Advanced Object Oriented Programming Language Lab.",
            s["body"],
        )
    )

    story.append(Spacer(1, 10))
    story.append(HRFlowable(width="100%", thickness=0.6, color=LINE, spaceAfter=8))

    sign = [
        [
            Paragraph("_______________________<br/>Al Araf Mahmud Samir<br/>0112230624", s["foot"]),
            Paragraph("_______________________<br/>Marfatul Hossain<br/>0112230701", s["foot"]),
            Paragraph("_______________________<br/>Taofik Ahamed<br/>0112230831", s["foot"]),
        ]
    ]
    t2 = Table(sign, colWidths=[60 * mm, 60 * mm, 60 * mm])
    t2.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("ALIGN", (0, 0), (-1, -1), "CENTER"),
            ]
        )
    )
    story.append(t2)
    story.append(Spacer(1, 8))
    story.append(
        Paragraph(
            "Submitted for evaluation and approval to the course faculty, Section A.",
            s["foot"],
        )
    )

    def footer(canvas, doc_):
        canvas.saveState()
        canvas.setStrokeColor(NAVY)
        canvas.setLineWidth(0.6)
        canvas.line(18 * mm, 11 * mm, A4[0] - 18 * mm, 11 * mm)
        canvas.setFont("Times-Roman", 8)
        canvas.setFillColor(MUTED)
        canvas.drawString(18 * mm, 7 * mm, "WorkSure — AOOP Language Lab | Section A | Group Project Proposal")
        canvas.drawRightString(A4[0] - 18 * mm, 7 * mm, "Page %d" % canvas.getPageNumber())
        canvas.restoreState()

    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    print(path)


if __name__ == "__main__":
    build()
